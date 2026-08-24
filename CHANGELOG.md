# Changelog

All notable changes to this project are documented here, following
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-08-24

### Added

- Resume ingestion for PDF, plain text and markdown, with PDFBox text extraction, per-file
  validation and a clear error for image-only PDFs that would need OCR.
- Structured extraction of contact details, skills, years of experience, roles held and education,
  through an LLM extractor with a deterministic rule-based twin behind the same interface.
- Job descriptions with explicit required skills, or skills inferred from the description text by a
  boundary-aware dictionary of 172 canonical skills and their aliases.
- Candidate scoring from 1 to 10 against a versioned rubric prompt, returning weighted sub-scores for
  skills, experience and education, matched and missing skills, and a written justification.
- Automatic per-candidate fallback to the deterministic scorer when the model is disabled,
  unreachable, rate limited or returns unusable JSON. Every stored verdict records which engine
  produced it.
- REST API documented with OpenAPI 3 and browsable through Swagger UI, with RFC 7807 error responses
  carrying a per-field breakdown on validation failures.
- A zero-build recruiter dashboard served by the application, with drag-and-drop upload, a live
  engine badge and ranked result cards.
- Persistence through Spring Data JPA over H2 in file mode, with resume deduplication by content
  hash and idempotent screening keyed on the resume and role pair.
- Test suite covering text extraction, the skill dictionary, both extraction engines, both scoring
  engines, defensive JSON parsing and an end-to-end run of the workflow over HTTP.
- GitHub Actions build, a multi-stage container image, and full documentation of the architecture,
  the prompts and the API.
