# 08 --- Contributing and Engineering Workflow

## 1. Workflow

1.  Read the relevant PRD requirement and roadmap ticket.
2.  Create a focused branch.
3.  Write or update tests.
4.  Implement the smallest coherent change.
5.  Run format, lint, test, and build commands.
6.  Review the diff for security and unrelated changes.
7.  Update API, schema, deployment docs, and ADRs when needed.
8.  Open a pull request or review the branch diff.
9.  Merge only after checks pass.

## 2. Branch and commit conventions

Examples: - `phase/01-foundation` - `feature/event-ingestion` -
`fix/tenant-scope-alert-query` - `docs/architecture-decisions`

Use descriptive commits,
e.g. `feat: add normalized event ingestion endpoint`.

## 3. Code expectations

-   Clear names and small cohesive modules.
-   No secrets in source, tests, fixtures, logs, or screenshots.
-   Validate external inputs at boundaries.
-   Use explicit error handling and bounded retries.
-   Test authorization and tenant isolation.
-   Avoid broad refactors unrelated to the ticket.
-   Keep dependency additions justified and documented.

## 4. Test expectations

-   Unit tests for business logic.
-   Integration tests for API/database behavior.
-   Security tests for authentication, authorization, and tenant
    boundaries.
-   Frontend tests for critical workflows.
-   Deterministic fixtures for detection rules.
-   Document manual tests that cannot be automated.

## 5. Pull request checklist

-   [ ] Ticket and acceptance criteria linked.
-   [ ] Tests added or updated.
-   [ ] Tests/build run and results reported.
-   [ ] No secrets or sensitive logs.
-   [ ] Authorization and tenant isolation reviewed.
-   [ ] API/schema docs updated.
-   [ ] Migrations tested.
-   [ ] No unrelated files changed.
-   [ ] Limitations and follow-up tickets recorded.

## 6. AI-assisted development policy

AI-generated code is untrusted until reviewed. The developer must
understand security-sensitive code, inspect diffs, verify dependencies,
and run tests. Do not approve changes solely because the agent says they
are correct.
