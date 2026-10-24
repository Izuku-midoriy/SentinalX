# 03 --- Database Design

## 1. Principles

-   PostgreSQL is the initial source of truth.
-   Use migrations; do not rely on automatic schema creation in
    production-like environments.
-   Store timestamps in UTC.
-   Use UUIDs or another consistent non-guessable identifier strategy.
-   Add `tenant_id` to every tenant-owned record and enforce tenant
    scope in application queries.
-   Never trust tenant identifiers supplied by the client.
-   Use foreign keys, check constraints, and indexes for critical
    invariants.
-   Avoid storing raw secrets or unnecessary sensitive log data.

## 2. Initial entities

### tenants

-   `id` UUID primary key
-   `name`
-   `slug` unique
-   `status`
-   `created_at`, `updated_at`

### users

-   `id` UUID primary key
-   `email` unique (normalized)
-   `password_hash` or external identity subject
-   `display_name`
-   `status`
-   `created_at`, `updated_at`, `last_login_at`

### tenant_memberships

-   `id`
-   `tenant_id` foreign key
-   `user_id` foreign key
-   `role`
-   unique (`tenant_id`, `user_id`)

### assets

-   `id`
-   `tenant_id`
-   `asset_name`
-   `asset_type`
-   `environment`
-   `last_seen_at`
-   `metadata` JSONB, size-limited
-   `created_at`, `updated_at`

### security_events

-   `id`
-   `tenant_id`
-   `event_time`
-   `ingested_at`
-   `source_type`
-   `source_name`
-   `event_type`
-   `severity`
-   `host`
-   `source_ip`, `destination_ip`
-   `username`, `process_name`, `process_id`
-   `message`
-   `raw_event` JSONB, subject to retention and size limits
-   `schema_version`
-   `idempotency_key` nullable
-   `created_at`

### detection_rules

-   `id`
-   `rule_key` unique
-   `name`, `description`
-   `enabled`
-   `severity`
-   `rule_type`
-   `configuration` JSONB validated against a known schema
-   `version`
-   `created_at`, `updated_at`

### alerts

-   `id`
-   `tenant_id`
-   `rule_id` foreign key
-   `title`, `description`
-   `severity`
-   `confidence` nullable
-   `status`
-   `first_seen_at`, `last_seen_at`
-   `assigned_to` nullable user FK
-   `deduplication_key`
-   `created_at`, `updated_at`

### alert_events

-   `alert_id` foreign key
-   `event_id` foreign key
-   composite primary key (`alert_id`, `event_id`)

### incidents

-   `id`
-   `tenant_id`
-   `title`, `description`
-   `severity`
-   `status`
-   `assigned_to` nullable user FK
-   `created_by` user FK
-   `created_at`, `updated_at`, `resolved_at`

### incident_alerts

-   `incident_id` foreign key
-   `alert_id` foreign key
-   composite primary key (`incident_id`, `alert_id`)

### incident_notes

-   `id`
-   `tenant_id`
-   `incident_id` foreign key
-   `author_id` user FK
-   `body` length-limited
-   `created_at`

### audit_logs

-   `id`
-   `tenant_id` nullable for platform-level events
-   `actor_user_id` nullable
-   `action`
-   `target_type`, `target_id`
-   `request_id`
-   `outcome`
-   `metadata` JSONB allowlisted and size-limited
-   `created_at`

### refresh_sessions (only if refresh-token/session design requires it)

-   `id`
-   `user_id`
-   `token_hash`
-   `expires_at`
-   `revoked_at`
-   `created_at`

## 3. Important indexes

-   `security_events (tenant_id, event_time DESC)`
-   `security_events (tenant_id, event_type, event_time DESC)`
-   `security_events (tenant_id, source_ip, event_time DESC)` where
    useful
-   `alerts (tenant_id, status, created_at DESC)`
-   `alerts (tenant_id, severity, created_at DESC)`
-   `incidents (tenant_id, status, updated_at DESC)`
-   `incident_notes (incident_id, created_at)`
-   `audit_logs (tenant_id, created_at DESC)`
-   unique constraints for membership and idempotency within the
    appropriate tenant/source scope

Confirm query plans before adding more indexes; every index has write
and storage costs.

## 4. Tenant isolation

-   Resolve tenant from authenticated membership or a scoped ingestion
    credential.
-   Include tenant scope in all reads, updates, deletes, searches,
    exports, and object lookups.
-   Use tests that attempt cross-tenant access by changing identifiers.
-   Consider PostgreSQL row-level security as an additional defense
    after the application-level model is working.
-   Platform-admin access to tenant data must be explicitly permissioned
    and audited.

## 5. Retention and privacy

-   Set a configurable event-retention period for the local MVP.
-   Avoid retaining unnecessary raw payloads.
-   Limit event/message and attachment sizes.
-   Document deletion and backup behavior.
-   Do not use real confidential logs in public demos or public
    repositories.
-   Before using a dataset, check its license and remove sensitive
    personal data as required.

## 6. Migration approach

-   Use versioned migrations.
-   Test migrations against a clean database and a database with prior
    data.
-   Back up before destructive changes.
-   Never let an agent drop or reset a database without explicit user
    approval.
