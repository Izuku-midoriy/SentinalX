# SentinelX — Initial Ticket Checklist

Use one ticket at a time with Antigravity. Mark a ticket complete only after verification.

## Phase 0 — Planning
- [x] SX-0001 Inspect existing repository and environment
- [x] SX-0002 Confirm architecture and zero-cost assumptions
- [x] SX-0003 Review and accept initial ADRs

## Phase 1 — Foundation
- [x] SX-0101 Create repository skeleton without overwriting existing work
- [x] SX-0102 Add `.gitignore` and `.env.example`
- [x] SX-0103 Scaffold React + TypeScript frontend
- [x] SX-0104 Scaffold Spring Boot backend and health endpoint
- [x] SX-0105 Add PostgreSQL Docker Compose profile
- [x] SX-0106 Add basic CI checks and setup documentation

## Phase 2 — Identity
- [x] SX-0201 Add tenant, user, membership, and role schema
- [x] SX-0202 Implement authentication and logout
- [x] SX-0203 Implement server-side permission checks
- [x] SX-0204 Add cross-tenant access tests
- [x] SX-0205 Add audit logging for sensitive actions

## Phase 3 — Ingestion
- [x] SX-0301 Define versioned normalized event schema
- [x] SX-0302 Add event validation and normalization
- [x] SX-0303 Implement authenticated event ingestion API
- [x] SX-0304 Add idempotency and size limits
- [x] SX-0305 Add sample-event generator/replay script

## Phase 4 — Detection
- [x] SX-0401 Implement brute-force rule
- [x] SX-0402 Implement second deterministic lab rule
- [x] SX-0403 Create evidence-linked alerts
- [x] SX-0404 Add deduplication and rule-version metadata
- [x] SX-0405 Add positive and negative detection tests

## Phase 5 — SOC UI
- [x] SX-0501 Build overview dashboard from API data
- [x] SX-0502 Build event search and filters
- [x] SX-0503 Build alert list and detail
- [x] SX-0504 Add pagination and loading/error/empty states
- [x] SX-0505 Verify UI and backend permissions

## Phase 6 — Incidents
- [x] SX-0601 Create incident from alert
- [x] SX-0602 Add assignment and status transitions
- [x] SX-0603 Add investigation notes and timeline
- [x] SX-0604 Audit incident mutations

## Phase 7 — Observability
- [x] SX-0701 Health, readiness, logs and metrics
- [x] SX-0702 Prometheus/Grafana local monitoring
- [x] SX-0801 Evaluate baseline ML model
- [x] SX-0802 Add bounded AI scoring integration
- [x] SX-0803 Review CIC-IDS2017 terms, labels and leakage risks
- [x] SX-0804 Train/evaluate Random Forest baseline
- [x] SX-0805 Compare XGBoost on the same split
- [x] SX-0806 Version model artifact and feature schema
- [x] SX-0807 Evaluate optional Qwen3 4B Instruct via Ollama for summaries
- [x] SX-0901 Add local IOC matching
- [ ] SX-1001 Measure need for Kafka/OpenSearch
- [ ] SX-1101 Test backup/restore and security hardening
- [ ] SX-1201 Add optional Kubernetes/Helm packaging
- [x] SX-1301 Prepare reproducible portfolio demo
