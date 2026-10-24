# ADR-0002: Zero-budget development and optional public hosting

-   Status: Accepted
-   Date: 2026-10-12

## Context

The developer requires SentinelX to be buildable without spending money.

## Decision

All core development and demonstration must work locally without paid
APIs, subscriptions, trial credits, or cloud accounts. Prefer
open-source software and local inference. Public hosting is optional and
only permitted after verifying current free-tier terms, quotas, and
resource limits.

## Alternatives

-   Make cloud hosting mandatory: rejected.
-   Depend on a paid LLM API: rejected.
-   Use local ML and local sample data: accepted for initial
    development.

## Consequences

-   Core demonstration remains reproducible.
-   Public availability may be limited or temporary on free hosting.
-   Full Kafka/OpenSearch/LLM workloads may exceed laptop or
    free-hosting resources.
-   Never claim that a public deployment has zero cost until its actual
    resource usage and provider terms are verified.
