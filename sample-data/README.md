# Sample data

Fictional data for trying the service or recording a demo. Every name, email address and phone
number here is invented.

| File | Purpose |
|---|---|
| `job-description.txt` | Senior Backend Engineer role - paste into the dashboard or the `POST /jobs` body |
| `resumes/priya_sharma.{txt,pdf}` | Strong match: 6 years of Java, Spring Boot, PostgreSQL, Kafka, Kubernetes |
| `resumes/arjun_reddy.{txt,pdf}` | Partial match: 3 years of Java and Spring Boot, no Kubernetes or Kafka |
| `resumes/rahul_mehta.{txt,pdf}` | Weak match: frontend developer, none of the required backend stack |

Each resume exists as both `.txt` and `.pdf` so you can demonstrate PDF parsing and still diff the
source text.

Expected ordering after screening against `job-description.txt`: **Priya > Arjun > Rahul**. That
ordering holds on both the LLM path and the deterministic path, which makes this a useful smoke test.

```bash
curl -X POST http://localhost:8080/api/v1/resumes \
  -F "files=@sample-data/resumes/priya_sharma.pdf" \
  -F "files=@sample-data/resumes/arjun_reddy.pdf" \
  -F "files=@sample-data/resumes/rahul_mehta.pdf"
```
