# Contributing

## Build and test

```bash
mvn clean verify      # compile, run the full suite, package
mvn test              # tests only
mvn spring-boot:run   # run locally on port 8080
```

The suite runs entirely offline. No test calls a real model: the LLM client is stubbed, which is why
CI is green without a secret configured.

## Conventions

- Java 17, four-space indent, no wildcard imports, no unused imports.
- Controllers stay thin. Business rules live in services, persistence in repositories.
- Every public class carries a Javadoc sentence explaining why it exists, not what it does.
- New behaviour arrives with a test. Bug fixes arrive with the test that would have caught them.

## Prompts

Prompts are files under `src/main/resources/prompts/`, never string literals in Java. Changing model
behaviour means editing a file that shows up in a diff. When you change a prompt, update
`docs/PROMPTS.md` in the same commit so the documented rubric never drifts from the shipped one.

## Commit messages

Conventional Commits: `feat(scope):`, `fix(scope):`, `docs:`, `test:`, `chore:`. One logical change
per commit.
