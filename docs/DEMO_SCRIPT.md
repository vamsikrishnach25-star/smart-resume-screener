# Demo video script (2-3 minutes)

A tight run-through that shows the product working and the engineering behind it. Record at
1920x1080, browser zoom around 110%, terminal font large enough to read.

## Before recording

```bash
export GEMINI_API_KEY="your-key"     # PowerShell: $env:GEMINI_API_KEY = "your-key"
rm -rf data/                          # start from an empty database
mvn spring-boot:run
```

Open two windows: the dashboard at <http://localhost:8080/> and a terminal.
Have `sample-data/` open in a file browser for drag and drop.

---

## 0:00 - 0:20 | What it is

> "This is Smart Resume Screener - a Spring Boot service that parses resumes, extracts structured
> candidate data, and ranks candidates against a job description using an LLM, with a written
> justification for every score."

Show the dashboard. Point at the badge in the top right: **LLM scoring: gemini-3.6-flash**.

## 0:20 - 0:50 | Ingest

Drag the three resumes from `sample-data/resumes/` onto the drop zone.

> "Each file is parsed independently. PDFBox pulls the text out, and the model turns it into
> structured JSON - name, contact details, skills, experience and education."

As the cards appear, point at the extracted skill chips and years of experience.

## 0:50 - 1:20 | The role

Paste `sample-data/job-description.txt` into the description box, set the title to
**Senior Backend Engineer** and minimum experience to **4**. Leave required skills blank.

> "I am leaving the required skills empty on purpose - the service infers them from the description
> using its skill dictionary."

Save, then show the inferred skill chips on the job card.

## 1:20 - 2:00 | Screen

Click **Screen candidates**.

> "For each candidate the service sends the resume and the role to the model with a rubric that
> defines what a 10, a 5 and a 1 mean, and asks for strict JSON back."

Walk through the top result: score ring, sub-scores for skills, experience and education, green
matched-skill chips, red missing-skill chips, and the justification underneath.

> "The justification cites the actual evidence, and the ranking puts the backend engineer above the
> frontend developer - which is the point."

## 2:00 - 2:30 | Engineering behind it

Switch to the terminal or IDE and show two things quickly:

1. `src/main/resources/prompts/job-match.system.txt`

   > "Prompts are versioned files, not string literals, so a prompt change goes through code review."

2. Stop the app, unset the key, restart, and screen again.

   > "With no API key the service does not fail - it falls back to a deterministic scorer, and every
   > stored verdict records which engine produced it. That is also why the whole test suite runs
   > offline in CI."

## 2:30 - 2:50 | API and tests

Show `http://localhost:8080/swagger-ui.html` scrolling the endpoint list, then:

```bash
mvn test
```

> "Unit tests for the parser, the dictionary and the scoring engines, plus an end-to-end test that
> walks the whole workflow through HTTP."

## 2:50 - 3:00 | Close

> "Everything - architecture, the full prompt text and the reasoning behind each rule - is documented
> in the README and the docs folder. Thanks for watching."

---

## Checklist before uploading

- [ ] No API key visible on screen at any point (check the terminal scrollback)
- [ ] Audio clear, no background noise
- [ ] Video is 3 minutes or less
- [ ] Link added to the README under **Demo video**
