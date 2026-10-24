# Antigravity Build Guide

## Purpose

Use Antigravity as an implementation agent for SentinelX, not as an
unrestricted autonomous developer. It should inspect the repository,
propose a bounded plan, make small changes, run checks, report evidence,
and wait for approval at phase boundaries.

## Workspace setup

Open the repository root as the Antigravity workspace. Keep this
structure:

``` text
SentinelX/
├── .agent/
│   ├── rules/
│   └── workflows/
├── docs/
│   └── 10-ADR/
├── frontend/
├── backend/
├── ai-service/
├── infra/
├── tests/
├── scripts/
├── .env.example
├── .gitignore
├── docker-compose.yml
└── README.md
```

Antigravity's exact configuration conventions can change by version. If
the installed version does not automatically load `.agent/rules/` or
`.agent/workflows/`, paste the contents of the project rules into its
project-level instructions and run the workflows as explicit prompts. Do
not invent configuration keys or assume a file was loaded---ask the
agent to confirm which project instructions it can see.

## Operating loop

For every stage:

1.  **Inspect:** list relevant files, existing code, commands, and
    current test status.
2.  **Plan:** provide a short implementation plan with files to
    create/change and risks.
3.  **Wait:** ask for approval before broad changes, migrations,
    destructive operations, or adding dependencies.
4.  **Implement:** work only on the approved stage; avoid unrelated
    refactors.
5.  **Verify:** run formatting, linting, unit tests, integration tests,
    and build checks applicable to the change.
6.  **Report:** list changed files, commands run, pass/fail results,
    limitations, and manual steps.
7.  **Checkpoint:** update the roadmap and ADRs when a decision changes.

## Rules for agent autonomy

-   The agent may read and edit source files and run non-destructive
    tests.
-   Require explicit approval before deleting files, resetting Git
    history, dropping databases, running destructive migrations,
    changing system security settings, exposing ports publicly,
    deploying externally, or spending money.
-   Do not paste secrets into prompts. Use `.env` locally and commit
    only `.env.example`.
-   Do not install unreviewed dependencies or silently change major
    versions.
-   Never run scans against third-party systems. Use local lab targets
    and authorized datasets only.
-   Never allow an AI-generated recommendation to execute shell commands
    or containment actions automatically.
-   Never report success based only on code generation; include
    test/build evidence.

## Model and context efficiency

-   Ask for one phase or one bounded ticket at a time.
-   Before coding, request a repository summary and identify
    assumptions.
-   Keep docs as the source of truth; if implementation conflicts with a
    document, report the conflict and propose an ADR.
-   Avoid generating every microservice up front. First deliver one
    end-to-end path: sample log → normalized event → detection → alert →
    dashboard.
-   Ask Antigravity to reuse existing working code rather than replace
    it.
-   Do not request a massive full-repository rewrite.

## Phase completion checklist

A phase is complete only when: - Acceptance criteria in
`docs/09-Roadmap.md` are satisfied. - Automated tests pass, or failures
are documented and justified. - No secrets are committed. - API and
schema changes are documented. - `README.md` setup instructions still
work. - The agent reports exact commands and results. - The user has
reviewed the diff and approved moving forward.

## Git checkpoint recommendation

Use a branch per meaningful phase, e.g. `phase/01-foundation`, and
commit after tests pass. Keep commits small and descriptive. Do not
force-push or rewrite history without explicit approval.
