# 01 --- Product Requirements Document (PRD)

-   Product: SentinelX
-   Document status: Baseline draft
-   Version: 1.0
-   Budget constraint: ₹0 required for development and local
    demonstration
-   Primary deployment target: Local Docker Compose environment
-   Audience: Student developer, reviewers, portfolio recruiters

## 1. Executive summary

SentinelX is a local-first, AI-assisted Security Operations Center
platform. It ingests security events, normalizes them, detects
suspicious patterns using transparent rules and optional ML, creates
alerts, and gives analysts a workflow to investigate and manage
incidents.

The first release prioritizes a reliable end-to-end detection path over
a large collection of unfinished microservices. The system must be
reproducible on a developer machine, documented, tested, secure by
default, and designed so heavier components can be added later.

## 2. Problem statement

Security teams receive events from different systems and need a unified
way to search activity, identify suspicious behavior, prioritize alerts,
and record investigations. Learning projects often show a dashboard with
static data but do not demonstrate a functioning pipeline. SentinelX
will demonstrate the complete flow from input event to evidence-backed
alert and incident.

## 3. Goals

1.  Ingest JSON events through a documented API and support a local log
    source.
2.  Normalize events to a versioned common event schema.
3.  Detect selected behaviors using explainable, tested rules.
4.  Create searchable alerts with severity, evidence, and rule metadata.
5.  Provide secure alert triage and incident workflows.
6.  Add optional ML scoring only after rule-based detection is reliable.
7.  Provide monitoring, tests, reproducible setup, and CI.
8.  Keep required development spend at zero.

## 4. Non-goals for initial release

-   Replacing commercial SIEM platforms.
-   Guaranteed enterprise throughput or 99.9% availability claims.
-   Fully automated endpoint containment or destructive response.
-   Scanning public targets or collecting data without authorization.
-   Mandatory managed cloud services, paid LLM APIs, paid threat feeds,
    or paid licenses.
-   Full multi-region disaster recovery in the MVP.
-   Deploying many microservices before a working vertical slice exists.

## 5. Target personas

### SOC Analyst

Searches events, triages alerts, reviews evidence, assigns incidents,
records notes, and closes incidents.

### SOC Manager

Reviews alert trends, incident status, workload, and detection quality.

### Tenant Administrator

Manages users and integrations within one organization.

### Platform Administrator

Operates the installation and manages tenant provisioning. Access to
tenant content must be controlled and audited.

### Auditor / Read-only Reviewer

Views permitted events, incidents, and audit records without modifying
them.

## 6. Core user journey

1.  Start SentinelX locally using documented commands.
2.  Sign in using a seeded development account that is disabled or
    safely isolated outside development.
3.  Submit a sample security event.
4.  Validate and normalize the event.
5.  Evaluate enabled detection rules.
6.  Create an alert with rule ID, severity, timestamps, and evidence.
7.  Search and open the alert in the UI.
8.  Assign the alert or create an incident.
9.  Add investigation notes and update status.
10. Review the audit trail and operational metrics.

## 7. Functional requirements

### Ingestion and normalization

-   FR-001: Accept authenticated JSON event submissions.
-   FR-002: Validate required fields, field types, maximum payload size,
    and timestamp format.
-   FR-003: Assign a unique event ID when the sender does not provide
    one.
-   FR-004: Normalize source-specific data to a versioned common event
    schema.
-   FR-005: Reject malformed input with safe, documented errors.
-   FR-006: Define duplicate handling and make ingestion idempotent
    where feasible.
-   FR-007: Provide documented seed events and a local replay script.

### Detection and alerting

-   FR-010: Implement a brute-force rule based on repeated failed
    authentication events in a configurable time window.
-   FR-011: Implement at least one additional rule, such as suspicious
    scan-like connection activity, using lab-generated events.
-   FR-012: Every alert must reference the triggering rule and
    supporting events.
-   FR-013: Alert severity and confidence must be separate fields.
-   FR-014: Detection decisions must be explainable and testable.
-   FR-015: Provide rule enable/disable configuration with authorization
    and audit logging.

### Search and dashboard

-   FR-020: Show alert counts by severity and status.
-   FR-021: Filter alerts by time, severity, status, source, and rule.
-   FR-022: Open alert details with evidence and related events.
-   FR-023: Display clearly labeled demo fixtures only where live data
    is unavailable.
-   FR-024: Handle loading, empty, error, and partial-data states.

### Incident management

-   FR-030: Create an incident from an alert.
-   FR-031: Assign incidents to permitted users.
-   FR-032: Support status transitions: Open, Investigating, Resolved,
    Closed.
-   FR-033: Add timestamped investigation notes.
-   FR-034: Maintain a timeline of incident changes.
-   FR-035: Audit assignment and status changes.

### Identity and administration

-   FR-040: Authenticate users securely.
-   FR-041: Enforce server-side role and tenant authorization.
-   FR-042: Prevent cross-tenant reads and writes.
-   FR-043: Support role assignments and account deactivation.
-   FR-044: Record security-sensitive administrative actions.

### AI/ML (later phase)

- Initial supervised dataset candidate: CIC-IDS2017 for network-flow classification only.
- Initial model comparison: scikit-learn Random Forest baseline versus XGBoost using a leakage-aware split and per-class metrics.
- Optional local LLM candidate: Qwen3 4B Instruct through Ollama, subject to model-tag, license, and hardware verification.
- The network-flow classifier must not be presented as a general-purpose classifier for arbitrary system logs.
- FR-050: Offer ML risk scoring only when a model is trained, validated, versioned, and documented.
- FR-051: Display model version and a human-readable explanation where possible.
- FR-052: AI summaries must cite available event/alert identifiers and distinguish evidence from inference.
- FR-053: Treat all event text as untrusted; do not execute model-generated commands.
- FR-054: Rule-based detection remains functional if the AI service is offline.
- Detailed training and evaluation requirements are defined in `11-AI-ML-Model-Strategy.md`.

### Operations

-   FR-060: Provide health/readiness endpoints.
-   FR-061: Provide structured logs with secrets redacted.
-   FR-062: Provide metrics for request latency, ingestion, detections,
    and failures.
-   FR-063: Provide backup and restore instructions for local data.
-   FR-064: Provide CI checks and reproducible setup instructions.

## 8. Non-functional requirements

-   Security: deny-by-default authorization, validated input, secrets
    outside source control, auditability.
-   Reliability: bounded retries, timeouts, clear failure states, and
    recoverable local setup.
-   Performance: establish a baseline with repeatable load tests; do not
    claim throughput until measured.
-   Maintainability: modules with clear ownership, tests, API contracts,
    migrations, and ADRs.
-   Accessibility: keyboard-operable primary workflows, visible focus,
    labels, and sensible contrast.
-   Portability: support a documented Windows 11 development workflow
    using Docker Desktop where feasible.
-   Cost: no paid dependency required to develop or demonstrate the core
    project locally.

## 9. Success criteria

-   A new developer can follow the README and start the MVP without paid
    credentials.
-   A sample failed-login sequence produces an alert deterministically.
-   Alert details show the rule and supporting events.
-   An analyst can create and update an incident.
-   Unauthorized users cannot access protected endpoints.
-   Tenant isolation has automated tests.
-   CI builds and tests the project.
-   Dashboard uses the actual backend API for implemented functionality.
-   Performance claims are backed by recorded test results.

## 10. Release definition

MVP is complete when authentication, ingestion, normalization,
rule-based detection, alerts, incident management, audit logging,
dashboard integration, tests, and local deployment work end-to-end. ML,
external feeds, Kafka, OpenSearch, and Kubernetes are staged additions,
not prerequisites for the first demo.
