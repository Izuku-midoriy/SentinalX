# 07 --- Deployment and Operations

## 1. Cost policy

Local development and the core demo must not require paid services, paid
APIs, trial credits, or cloud resources. Public hosting is optional and
must be checked against current free-tier limits before use. Free
software does not guarantee free compute, bandwidth, storage, or
always-on hosting.

## 2. Local baseline

Use Docker Compose for PostgreSQL and application dependencies. Start
with the smallest useful profile. Add OpenSearch, Kafka, Redis, AI
inference, and monitoring in stages rather than starting every container
immediately.

Prerequisites: - Git - Docker Desktop (observe its current license
terms) - Java 21 JDK for local backend work if not containerized -
Python supported by the chosen dependency versions - Node.js LTS -
Adequate free disk space and RAM

## 3. Configuration

-   Commit `.env.example` with placeholders only.
-   Keep `.env` ignored by Git.
-   Validate required configuration on startup.
-   Never bake secrets into container images or frontend bundles.
-   Use separate development and production profiles.
-   Provide safe development-only seed data and credentials; never reuse
    them publicly.

## 4. Health and observability

Provide: - Liveness endpoint - Readiness endpoint that checks required
dependencies - Structured application logs - Metrics endpoint protected
appropriately - Prometheus scrape configuration - Grafana dashboard
provisioning where useful

Do not expose database, metrics, or admin ports publicly by default.

## 5. CI/CD

GitHub Actions free-tier usage is subject to current quotas. CI should
run: - Formatting/lint checks - Backend unit and integration tests -
Frontend lint, type check, and tests - AI-service tests -
Dependency/security checks where practical - Container build validation

Do not deploy automatically to a paid resource. Public deployment should
require explicit configuration and approval.

## 6. Data durability

Document: - Database backup command - Restore procedure - Migration
procedure - Event-retention policy - How to reset a development
environment safely

Test restore into a separate local database. Never let an automation
delete the only copy of user data.

## 7. Scaling and Kubernetes

Kubernetes manifests and Helm charts are a later deliverable. They can
be built and validated locally, but running a production-like cluster
may consume substantial resources. Do not claim Kubernetes production
readiness merely because manifests exist.

## 8. Public demo checklist

-   No real personal or confidential logs.
-   No default development credentials.
-   TLS and secure headers configured.
-   Authentication and authorization verified.
-   Rate limits and payload limits configured.
-   No admin/database/metrics ports exposed.
-   Backups and data deletion documented.
-   Free hosting quota and terms verified.
-   Cost alerts or spending limits configured if the provider offers
    them.
