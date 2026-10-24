# 09 --- Staged Implementation Roadmap

## Delivery strategy

Build a vertical slice early. Each phase must leave the repository in a
runnable state. Do not ask Antigravity to implement all phases in one
prompt.

## Phase 0 --- Repository and planning baseline

**Deliverables:** repository inspection, confirmed stack, ADR-0001,
project skeleton proposal, acceptance criteria. **Acceptance:** no
application code changed without approval; known prerequisites and
resource constraints documented.

## Phase 1 --- Local foundation

**Deliverables:** repository structure, README, `.gitignore`,
`.env.example`, Docker Compose for PostgreSQL, frontend scaffold, Spring
Boot scaffold, Python AI-service scaffold only if needed, basic CI.
**Acceptance:** frontend and backend start locally; database health
check passes; secrets are ignored; CI runs basic checks.

## Phase 2 --- Identity and authorization

**Deliverables:** users, tenants, memberships, roles/permissions,
login/logout, secure session strategy, audit events. **Acceptance:**
unauthenticated protected requests fail; role tests pass; cross-tenant
access tests pass; no default production credentials.

## Phase 3 --- Event contract and ingestion

**Deliverables:** versioned event DTO/schema, validation, normalization,
ingestion endpoint, database migration, sample-event script.
**Acceptance:** valid event is stored; invalid and oversized payloads
are rejected; tenant comes from authenticated context; idempotency
behavior is tested.

## Phase 4 --- Rule-based detection and alerts

**Deliverables:** brute-force detection rule, second simple rule,
evidence linking, alert lifecycle, deduplication. **Acceptance:**
deterministic test events trigger expected alerts; benign events do not
trigger those rules; alert records link to evidence and rule version.

## Phase 5 --- Event search and SOC dashboard

**Deliverables:** overview, event search, alert list/detail, real API
integration, loading/empty/error states. **Acceptance:** UI displays
stored data; filters and pagination work; API failures are visible; no
fake data masks failures.

## Phase 6 --- Incident management

**Deliverables:** create incident from alert, assignment, notes, status
transitions, timeline, audit. **Acceptance:** authorized analyst can
investigate; unauthorized user cannot modify; actions appear in
timeline/audit log.

## Phase 7 --- Observability

**Deliverables:** health/readiness endpoints, structured logs,
Prometheus metrics, Grafana dashboard. **Acceptance:** ingestion counts,
errors, request latency and detection counts can be observed locally;
secrets are not logged.

## Phase 8 — ML-assisted risk scoring

**Deliverables:** documented baseline dataset, preprocessing pipeline,
model evaluation, model versioning, optional FastAPI inference endpoint,
fallback behavior.

**Acceptance:** metrics are reproducible; train/test leakage is avoided;
model output is not confused with deterministic rule evidence; the app works
if the AI service is offline.

The ML phase must remain after the core ingestion, rule-based detection,
alerts, and dashboard work. Do not make ML a prerequisite for the first MVP.

## Phase 9 --- Threat intelligence

**Deliverables:** local IOC dataset and/or a permitted free feed,
normalization, matching, source and freshness metadata. **Acceptance:**
known test IOC matches; stale/unavailable feeds fail safely; provider
limits and license are documented.

## Phase 10 --- Event streaming and search scale

**Deliverables:** optional Kafka pipeline and optional OpenSearch
indexing. **Acceptance:** duplicate delivery, retry, consumer restart,
backpressure, and indexing failures are tested. Keep a simple local
profile available.

## Phase 11 --- Hardening and reporting

**Deliverables:** PDF/CSV reports, retention, backup/restore, dependency
scans, rate limits, security review. **Acceptance:** restore is tested;
report access is authorized; exports are bounded; known risks
documented.

## Phase 12 --- Deployment packaging

**Deliverables:** production-like container configuration, optional
Helm/Kubernetes manifests, CI build pipeline, operations guide.
**Acceptance:** clean setup works from documentation; health checks
pass; no secrets baked into images; resource needs and limits
documented.

## Phase 13 --- Portfolio release

**Deliverables:** architecture diagrams, screenshots using demo data,
recorded demo steps, final README, limitations, measured benchmark
results. **Acceptance:** a reviewer can run the project and reproduce
the core event-to-alert-to-incident workflow.

## Definition of done for every ticket

-   Acceptance criteria are met.
-   Tests pass or failures are explicitly explained.
-   No secrets are committed.
-   Documentation is updated.
-   Diff is reviewed.
-   Agent reports changed files, commands and results.
-   User approves the next phase.
