# Architecture

## Goals

1. Turn unstructured resumes into structured, queryable candidate data.
2. Rank candidates against a role with a score a human can act on and defend.
3. Stay available and testable when the language model is not.

## Layers

| Layer | Package | Responsibility |
|---|---|---|
| Web | `controller` | HTTP contract, validation entry point, OpenAPI metadata. No business logic. |
| Application | `service` | Orchestration: ingestion, screening, engine selection, persistence boundaries. |
| Extraction | `service.extraction` | File to text (PDFBox), text to structured data (LLM or rules), skill dictionary. |
| Scoring | `service.scoring` | Candidate to score, via the model or deterministic arithmetic. |
| LLM access | `service.llm` | Provider client, prompt loading, defensive JSON parsing. |
| Domain | `model` | JPA entities and the enums that keep results auditable. |
| Persistence | `repository` | Spring Data JPA interfaces. |

Dependencies point inwards: controllers depend on services, services depend on extraction/scoring,
and only `service.llm` knows a provider exists. Swapping Gemini for another provider means adding one
class that implements `LlmClient`.

## The two-engine design

Every model-backed capability has a deterministic twin.

```
                    ┌──────────────────────────┐
  resume text  ───▶ │ ResumeExtractionService  │
                    └───────────┬──────────────┘
                    LLM ok?     │
                 ┌──────────────┴───────────────┐
                 ▼                              ▼
        LlmResumeExtractor            HeuristicResumeExtractor
        (Gemini + JSON schema)        (regex + sections + dictionary)


                    ┌──────────────────────────┐
  resume + job ───▶ │     ScreeningService     │
                    └───────────┬──────────────┘
                    LLM ok?     │
                 ┌──────────────┴───────────────┐
                 ▼                              ▼
          LlmMatchScorer                 HeuristicMatchScorer
        (rubric + justification)        (weighted skill coverage)
```

Selection happens per operation, not per application start:

- If `app.llm.enabled` is false or no API key is present, the deterministic engine is used directly.
- If a model call fails (network error, 429, 5xx after retries, unparseable JSON), that single
  operation falls back; the rest of the batch still tries the model.
- The engine that produced a result is stored on the row as `analysis_source`, and surfaced in the
  API and dashboard.

## Request flows

### Upload

```
POST /api/v1/resumes (multipart)
  └─ ResumeService.upload
       ├─ per file: DocumentTextExtractor.extract      # type + size validation, PDFBox, normalise
       ├─ SHA-256 of extracted text  ──▶ already stored? reuse, no model call
       ├─ ResumeExtractionService.extract              # LLM, else rules
       └─ ResumeRepository.save                        # resume + skills + experience + education
  ⇒ 201 with parsed resumes and a reason per rejected file
```

A failure on one file never fails the batch: parsing exceptions are collected into
`UploadResponse.rejected`.

### Screening

```
POST /api/v1/screenings
  └─ ScreeningService.screen
       ├─ JobDescriptionService.requireJob             # 404 if unknown
       ├─ resolve candidates (explicit ids, or all stored resumes)
       ├─ per candidate:
       │     ├─ stored verdict for (resume, job)? reuse unless refresh=true
       │     ├─ LlmMatchScorer.score  ──(LlmException)──▶ HeuristicMatchScorer.score
       │     └─ save ScreeningResult                   # unique (resume_id, job_id)
       └─ sort by score, apply minScore/topN, assign ranks
  ⇒ 200 with a ranked shortlist
```

## Persistence

H2 in file mode (`./data/screener`) with `ddl-auto: update`, so a reviewer gets a working database
with no setup and data survives restarts. Nothing in the code is H2-specific: switching to PostgreSQL
is a driver dependency plus a datasource URL.

Collections (`skills`, `experience`, `education`, matched/missing skills) are mapped as JPA element
collections with their own tables, keeping the schema normalised and queryable rather than dumping
JSON blobs into a column.

`screening_results` has a unique constraint on `(resume_id, job_id)`. That single constraint gives
idempotent screenings, cheap re-reads, and a natural cache key.

## Error handling

`GlobalExceptionHandler` maps every exception onto an RFC 7807 `ProblemDetail`:

| Exception | Status | Meaning |
|---|---|---|
| `ResourceNotFoundException` | 404 | Unknown resume, job or result id |
| `DocumentParsingException` | 422 | Unsupported type, empty file, or no selectable text |
| `MethodArgumentNotValidException` | 400 | Bean validation failure, with a `fieldErrors` map |
| `IllegalArgumentException` | 400 | Bad request semantics, e.g. screening with no stored resumes |
| `MaxUploadSizeExceededException` | 413 | File beyond the servlet limit |
| anything else | 500 | Logged with a stack trace, generic message to the client |

`LlmException` deliberately never reaches the client: it is caught by the service layer and turned
into a fallback.

## Security considerations

- Resume and job text are passed to the model as data, and the system prompt instructs the model to
  ignore instructions found inside them (prompt injection defence).
- The API key is read from the environment, sent in the `x-goog-api-key` header rather than a query
  string, and never logged.
- Uploads are constrained by extension, per-file size and per-request count before any parsing runs.
- CORS is open for `/api/**` to keep local development simple; a deployment should narrow it and put
  authentication in front of the API.

## What would change for production

| Concern | Today | Production |
|---|---|---|
| Database | H2 file | PostgreSQL, Flyway migrations |
| Screening | Synchronous | Async job + polling or webhooks for large batches |
| Scanned PDFs | Rejected | OCR step behind `DocumentTextExtractor` |
| Auth | None | OIDC or API keys, per-recruiter data scoping |
| Candidate data | Stored indefinitely | Retention policy, consent tracking, deletion endpoint (partially present) |
| Observability | Logs | Metrics on model latency, fallback rate, score distribution |
