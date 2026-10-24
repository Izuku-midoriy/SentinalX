# 02 --- Technical Architecture

## 1. Architecture decision

Start with a **modular monolith plus a separate optional Python AI
service**. This reduces operational overhead while preserving boundaries
that can later be extracted into services. Kafka, OpenSearch, and
Kubernetes are not mandatory for the first local MVP; introduce them in
later phases after measuring a real need.

## 2. Logical architecture

``` text
Analyst Browser
    |
    v
React + TypeScript UI
    |
    v
Spring Boot REST API
    |-- Identity and Access module
    |-- Tenant and User module
    |-- Ingestion and Normalization module
    |-- Detection Rules module
    |-- Alerts module
    |-- Incident Management module
    |-- Audit module
    |-- Metrics / Health endpoints
    |
    +--> PostgreSQL (users, tenants, events, alerts, incidents, audit)
    |
    +--> Optional OpenSearch (event search at scale)
    |
    +--> Optional Kafka (buffering and asynchronous processing)
    |
    +--> Optional Python FastAPI AI service (ML scoring and summaries)
    |
    +--> Prometheus --> Grafana
```

## 3. Initial deployment profile

Required: - Frontend - Spring Boot backend - PostgreSQL - Optional local
reverse proxy

Recommended later: - Python AI service - Prometheus and Grafana -
Redis - OpenSearch - Kafka

Run only the components needed for the current phase. Heavy services may
overwhelm a developer laptop if started together.

## 4. Module boundaries

-   Identity: authentication, sessions/tokens, roles, permissions.
-   Tenancy: organizations and tenant membership.
-   Ingestion: API validation, source metadata, normalization,
    idempotency.
-   Detection: rule definitions, rule execution, match evidence.
-   Alerts: alert lifecycle, status, severity, assignments.
-   Incidents: incident lifecycle, notes, relationships to alerts.
-   Audit: append-only business audit events.
-   AI adapter: bounded calls to a separately running AI service; must
    degrade gracefully.
-   Operations: health, readiness, metrics, structured logs.

Modules may live in one Spring Boot deployable initially. Avoid circular
dependencies.

## 5. Event processing flow

1.  Client submits an event using a documented API.
2.  API authenticates the sender and enforces payload limits.
3.  Validator checks schema and timestamp.
4.  Normalizer maps fields to the common event schema.
5.  Idempotency logic handles repeated submissions.
6.  Event is stored.
7.  Enabled detection rules evaluate the event and relevant recent
    history.
8.  Matching rules create alerts with evidence references.
9.  Audit and metrics record the processing outcome.
10. UI queries alerts and event details.

Later, Kafka can decouple ingestion from detection. Preserve the event
contract when changing transport.

## 6. Common event schema (initial)

Required or recommended fields: - `event_id`: unique ID - `tenant_id`:
derived from authenticated context, not trusted from user input -
`timestamp`: UTC ISO 8601 timestamp - `source_type`: linux, windows,
web, firewall, cloud, or custom - `source_name`: agent or integration
identifier - `event_type`: normalized event category - `severity`:
normalized informational severity when available - `host`: asset name or
ID - `source_ip`, `destination_ip`: optional validated addresses -
`username`, `process_name`, `process_id`: optional source fields -
`message`: bounded, untrusted source text - `raw_event`: optional
original payload subject to size and privacy policy - `schema_version`:
version of this contract

Do not accept `tenant_id` as authoritative from an untrusted sender.
Resolve it from the authenticated principal or ingestion credential.

## 7. Storage strategy

-   PostgreSQL: system of record for identity, tenants, events in the
    MVP, alerts, incidents, notes, audit records, rule metadata.
-   OpenSearch: optional indexed event search when volume and query
    requirements justify it.
-   Kafka: optional event transport/buffering, not the source of truth.
-   Redis: optional cache/rate-limit support; correctness must not
    depend on cache contents.
-   Object storage: optional evidence attachments with strict file type,
    size, and access controls.

## 8. AI service boundaries

The AI service must: - Accept only authenticated, bounded requests. -
Receive the minimum necessary event/alert context. - Return structured
results with model/version metadata. - Have timeouts and response
validation. - Never decide authorization. - Never execute generated
commands. - Never be required for rule-based detection. - Mark generated
summaries as machine-generated and potentially incomplete.

### 8.1 Initial model choices

- Random Forest (scikit-learn) is the first supervised baseline for CIC-IDS2017 network-flow classification.
- XGBoost is a comparison candidate, not a predetermined winner.
- Qwen3 4B Instruct through Ollama is an optional local LLM candidate for evidence-grounded investigation summaries, subject to license and hardware checks.
- These models serve different purposes. Do not average their scores.
- Full training, evaluation, provenance, and inference requirements are in `docs/11-AI-ML-Model-Strategy.md`.

The architecture must preserve the boundary between the Java/Spring Boot core,
the optional Python FastAPI model service, and any local LLM. Rule-based
detection must continue to work if AI services are unavailable.

## 9. Scalability path

1.  Optimize and measure the modular monolith.
2.  Add indexes, pagination, retention, and bounded query windows.
3.  Add asynchronous processing if synchronous ingestion becomes a
    bottleneck.
4.  Add Kafka for buffering and independent consumers.
5.  Add OpenSearch for high-volume event search.
6.  Extract services only where scaling/ownership justify it.
7.  Add Kubernetes after containerized local deployment and health
    checks are stable.

## 10. Observability

Metrics: - API request count, latency, and errors - Events
accepted/rejected - Detection evaluations and matches - Alert creation
latency - Database pool usage - Queue lag if Kafka is enabled - AI
request duration/errors if enabled

Logs must be structured and redact secrets and sensitive values. Trace
IDs should connect API requests to downstream processing where
practical.
