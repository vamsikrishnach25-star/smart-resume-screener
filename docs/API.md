# API reference

Base URL: `http://localhost:8080/api/v1`
Interactive docs: `http://localhost:8080/swagger-ui.html`

All responses are JSON. Errors use RFC 7807 (`application/problem+json`).

---

## Resumes

### `POST /resumes`

Upload one or more resumes. `multipart/form-data`, repeated field name `files`.
Accepted: `.pdf`, `.txt`, `.md`, up to 5 MB each and 25 files per request.

```bash
curl -X POST http://localhost:8080/api/v1/resumes \
  -F "files=@sample-data/resumes/priya_sharma.pdf" \
  -F "files=@sample-data/resumes/rahul_mehta.txt"
```

**201 Created**

```json
{
  "received": 2,
  "parsed": 2,
  "resumes": [
    {
      "id": "8c1f...",
      "fileName": "priya_sharma.pdf",
      "candidateName": "Priya Sharma",
      "email": "priya.sharma@example.com",
      "phone": "+91 98765 43210",
      "yearsOfExperience": 6.0,
      "highestEducation": "B.Tech in Computer Science",
      "skills": ["Java", "Spring Boot", "PostgreSQL", "Docker", "Kubernetes", "Apache Kafka"],
      "extractionSource": "LLM",
      "createdAt": "2026-08-23T09:14:22.113Z"
    }
  ],
  "rejected": []
}
```

A file that cannot be parsed appears in `rejected` with a reason; the rest of the batch still succeeds:

```json
{ "rejected": [{ "fileName": "scan.pdf", "reason": "No readable text found in 'scan.pdf'. Scanned or image-only PDFs need OCR before they can be screened." }] }
```

### `GET /resumes`

List every parsed resume, newest first. Returns an array of the summary object shown above.

### `GET /resumes/{id}`

Full profile: adds `location`, `summary`, `experiences[]`, `educations[]`, `contentType`,
`fileSizeBytes` and a 1,200-character `rawTextPreview`.

**404** when the id is unknown.

### `DELETE /resumes/{id}`

Deletes the resume and every screening result that references it. **204 No Content**.

---

## Job descriptions

### `POST /jobs`

```json
{
  "title": "Senior Backend Engineer",
  "company": "Unthinkable",
  "location": "Gurugram, India",
  "minYearsExperience": 4,
  "description": "Build and scale microservices in Java and Spring Boot backed by PostgreSQL...",
  "requiredSkills": ["Java", "Spring Boot", "PostgreSQL", "Docker", "Kubernetes"]
}
```

| Field | Rules |
|---|---|
| `title` | required, max 200 characters |
| `description` | required, 30-40,000 characters |
| `minYearsExperience` | optional, >= 0 |
| `requiredSkills` | optional; when omitted or empty, skills are inferred from the description |

**201 Created** returns the stored role including the resolved `requiredSkills` and `createdAt`.

**400** on validation failure:

```json
{
  "type": "https://smart-resume-screener/errors/400",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "timestamp": "2026-08-23T09:20:04.882Z",
  "fieldErrors": {
    "title": "title is required",
    "description": "description must be between 30 and 40000 characters"
  }
}
```

### `GET /jobs`, `GET /jobs/{id}`, `DELETE /jobs/{id}`

List, fetch and delete. Deleting a role also removes its screening results.

---

## Screening

### `POST /screenings`

```json
{
  "jobDescriptionId": "0f2a...",
  "resumeIds": null,
  "minScore": 5.0,
  "topN": 10,
  "refresh": false
}
```

| Field | Meaning |
|---|---|
| `jobDescriptionId` | required |
| `resumeIds` | optional subset; omit or send `null` to screen every stored resume |
| `minScore` | optional 0-10 floor applied after scoring |
| `topN` | optional cap on returned candidates |
| `refresh` | `true` re-scores pairs that already have a stored verdict |

**200 OK**

```json
{
  "jobDescriptionId": "0f2a...",
  "jobTitle": "Senior Backend Engineer",
  "candidatesEvaluated": 3,
  "candidatesReturned": 2,
  "shortlistedCount": 1,
  "engine": "gemini:gemini-3.6-flash",
  "results": [
    {
      "rank": 1,
      "resumeId": "8c1f...",
      "candidateName": "Priya Sharma",
      "fileName": "priya_sharma.pdf",
      "email": "priya.sharma@example.com",
      "yearsOfExperience": 6.0,
      "matchScore": 9.1,
      "skillScore": 9.5,
      "experienceScore": 9.0,
      "educationScore": 8.0,
      "recommendation": "STRONG_MATCH",
      "shortlisted": true,
      "matchedSkills": ["Java", "Spring Boot", "PostgreSQL", "Docker", "Kubernetes"],
      "missingSkills": [],
      "justification": "Six years building distributed Java services...",
      "analysisSource": "LLM",
      "model": "gemini-3.6-flash",
      "latencyMs": 2140,
      "evaluatedAt": "2026-08-23T09:22:41.004Z"
    }
  ]
}
```

`recommendation` bands: `STRONG_MATCH` (>= 8.5), `GOOD_MATCH` (>= 7.0), `POSSIBLE_MATCH` (>= 5.0),
`NOT_A_MATCH` (< 5.0). `shortlisted` is true for the first two bands.

`analysisSource` is `LLM` or `HEURISTIC`, so a reviewer can always tell which engine produced a score.

### `GET /screenings/jobs/{jobId}`

Returns the stored ranking for a role without re-scoring anything.

---

## System

### `GET /system/status`

```json
{
  "application": "Smart Resume Screener",
  "version": "1.0.0",
  "llmEnabled": true,
  "llmConfigured": true,
  "provider": "gemini",
  "model": "gemini-3.6-flash",
  "activeEngine": "gemini:gemini-3.6-flash",
  "storedResumes": 3,
  "storedJobs": 1
}
```

Used by the dashboard badge to show whether scoring runs on the model or the deterministic engine.
