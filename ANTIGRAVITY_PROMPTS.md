# Antigravity Copy/Paste Prompts

Use one prompt at a time. Do not paste all prompts into a single
request.

## Prompt 0 --- Inspect and plan only

You are the implementation agent for SentinelX. Read
`ANTIGRAVITY_BUILD_GUIDE.md`, `.agent/rules/sentinelx-project-rules.md`,
and the docs under `docs/`. First inspect the current workspace and
report the existing files, runtimes, dependencies, and test commands. Do
not modify files yet. Identify conflicts, missing decisions, and laptop
resource risks. Propose a Phase 0 plan and list exactly which files you
would create or change. Respect the ₹0 development constraint. Wait for
approval.

## Prompt 1 --- Phase 0 documentation baseline

Implement only Phase 0 from `docs/09-Roadmap.md`. Inspect before
editing. Create/update the repository skeleton proposal and ADRs as
needed, but do not implement application features. Do not overwrite
existing work. Run documentation checks if available. Report changed
files and wait.

## Prompt 2 --- Phase 1 foundation

Implement only Phase 1 in `docs/09-Roadmap.md`. Use the chosen stack in
`docs/02-Architecture.md`. Create a minimal runnable React frontend,
Spring Boot backend, PostgreSQL local profile, `.env.example`,
`.gitignore`, Docker Compose, and basic CI checks. Keep containers
minimal; do not add Kafka/OpenSearch/Kubernetes yet. Do not include real
secrets. Run build/tests and report exact commands/results. Stop before
Phase 2.

## Prompt 3 --- Phase 2 identity

Implement only Phase 2. First propose the authentication/session design
and migration plan. Enforce server-side authorization and tenant
isolation. Add tests for unauthenticated access, role permissions, and
cross-tenant reads/writes. Do not seed a production default password.
Ask before destructive database operations. Stop after tests and report.

## Prompt 4 --- Phase 3 ingestion

Implement only Phase 3. Define the event DTO/schema, validation,
normalization, tenant resolution, idempotency behavior, migration, and
sample event script. Do not trust client-supplied tenant IDs. Add tests
for valid, invalid, oversized, duplicate, and cross-tenant event
submissions. Keep event storage in PostgreSQL initially. Stop after
tests.

## Prompt 5 --- Phase 4 detection

Implement the brute-force rule and one additional deterministic lab rule
from the roadmap. Keep thresholds configurable and detection
explanations inspectable. Link alerts to supporting event IDs and rule
versions. Add positive and negative tests. Use synthetic/local lab
events only; do not attack any real system. Stop after tests.

## Prompt 6 --- Phase 5 dashboard

Implement the overview, event search, alert list and alert detail pages
using the real backend API. Add loading, empty, error, filter, and
pagination states. Do not silently use mock data when the API fails.
Verify that tenant permissions are enforced by the backend. Run frontend
checks and relevant integration tests.

## Prompt 7 --- Phase 6 incidents

Implement incident creation from an alert, assignment, notes, permitted
status transitions, timeline, and audit events. Test authorized and
unauthorized mutations. Do not add automated endpoint containment. Stop
after tests.

## Prompt 8 --- Phase 7 monitoring

Add health/readiness endpoints, structured redacted logs, Prometheus
metrics and a basic Grafana dashboard. Keep monitoring local and free.
Do not expose metrics/admin ports publicly. Verify that metrics work and
secrets are not logged.

## Prompt 9 --- Phase 8 ML

Before coding, propose a baseline model, dataset/license review,
preprocessing, train/validation/test strategy, evaluation metrics, and
resource estimate. Avoid train/test leakage. Add a separate AI service
only if needed. Include model version and fallback behavior. Never allow
model output to execute commands or make authorization decisions. Wait
for approval of the plan before implementation.

## Prompt 10 --- Phase 9 threat intelligence

Implement a local IOC dataset or a permitted free feed. Record source
and freshness, validate indicators, and avoid uncontrolled external
requests. Add deterministic tests for match, no-match, malformed, stale,
and unavailable-feed behavior. Do not add a paid provider.

## Prompt 11 --- Phase 10 scale components

Do not add Kafka or OpenSearch merely for appearance. First inspect
measured bottlenecks and propose the smallest justified change. If
approved, implement one component at a time with a simple local
fallback. Test duplicate delivery, retries, backpressure, restart
recovery, and index failures.

## Prompt 12 --- Hardening and release

Review the whole repository against `docs/05-Security.md` and the
release checklist. Add missing tests and docs, dependency checks,
backup/restore steps, bounded exports, and deployment checks. Do not
claim compliance certification or production readiness without evidence.
Report risks and gaps honestly.
