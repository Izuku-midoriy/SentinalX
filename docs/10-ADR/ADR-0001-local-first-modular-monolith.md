# ADR-0001: Local-first modular monolith for the MVP

-   Status: Accepted
-   Date: 2026-10-12

## Context

SentinelX must be built with a zero-budget development constraint and
demonstrated on a developer laptop. Starting with many independent
services plus Kafka, OpenSearch, Kubernetes, and a local LLM would add
resource and debugging overhead before the core workflow exists.

## Decision

Start with a modular Spring Boot backend, PostgreSQL, and a React
frontend. Keep module boundaries explicit. Add a separate Python service
only when an ML/AI feature needs it. Add Kafka, OpenSearch, Redis, and
Kubernetes in later phases based on demonstrated need.

## Alternatives

-   Start with many microservices: rejected for MVP due to operational
    overhead.
-   Use a single unstructured application: rejected because it weakens
    boundaries and later evolution.
-   Depend on managed cloud services: rejected because the project must
    be buildable locally at zero cost.

## Consequences

-   Faster path to a working vertical slice.
-   Fewer containers and lower memory use early.
-   Some later extraction work may be required, but stable contracts
    reduce that cost.
-   Scalability claims must be supported by measured testing rather than
    architecture diagrams alone.

## Security implications

-   Clear module boundaries do not replace authorization checks.
-   Tenant scoping and audit logging must be implemented before
    multi-user use.
