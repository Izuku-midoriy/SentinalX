# 05 --- Security and Access Control

## 1. Security objectives

Confidentiality, integrity, availability, tenant isolation, explainable
detections, safe analyst workflows, and auditable administrative
actions.

## 2. Roles

-   Platform Admin: platform configuration and tenant lifecycle;
    tenant-content access is separately controlled.
-   Tenant Admin: manages users and integrations inside the tenant.
-   SOC Manager: manages incidents, assignments, and team-level views.
-   SOC Analyst: investigates permitted alerts and incidents.
-   Auditor: read-only access to specifically authorized records.

Define permissions explicitly rather than relying only on role names.
Default to deny.

## 3. Threat model

Protect against: - Broken access control and cross-tenant data
exposure - Credential theft, brute force, session abuse - SQL injection,
XSS, CSRF, SSRF, and unsafe deserialization - Oversized or malformed
event payloads - Log injection and sensitive data leaking through logs -
Malicious uploaded evidence - Prompt injection embedded in event
messages - Poisoned or misleading threat-intelligence data - Resource
exhaustion from event floods or expensive searches - Secret leakage
through Git, container images, or CI logs - Unauthorized or accidental
response actions

## 4. Authentication and sessions

-   Use a maintained Spring Security configuration.
-   Store passwords only as strong password hashes using a suitable
    adaptive algorithm.
-   Rate-limit login attempts and use generic authentication failure
    messages.
-   Use secure, HttpOnly, SameSite cookies if using browser sessions;
    implement CSRF protections as appropriate.
-   If using bearer tokens, define short access-token lifetimes, refresh
    rotation/revocation, secure storage, and logout behavior.
-   Add MFA before presenting the system as production-ready.
-   Never commit real credentials or use production secrets in
    development fixtures.

## 5. Authorization and tenant isolation

-   Enforce authorization in service and data-access layers.
-   Scope all tenant-owned records to the authenticated tenant.
-   Check ownership before returning whether a specific object exists
    where existence itself is sensitive.
-   Test cross-tenant access for every object endpoint and mutation.
-   Audit role changes, user deactivation, integration changes, and
    sensitive incident actions.
-   Do not rely on frontend route guards as a security control.

## 6. API and data protection

-   Validate payload schema, content type, size, and rate.
-   Use parameterized queries and safe ORM patterns.
-   Apply CORS allowlists; do not use wildcard origins with credentials.
-   Add secure headers and a restrictive Content Security Policy
    appropriate to the frontend.
-   Redact secrets and sensitive personal data from logs.
-   Keep dependency versions maintained and run dependency scanning in
    CI where free tooling permits.
-   Use TLS for public deployments; local loopback development may use
    HTTP.
-   Limit raw-event retention and evidence upload size/type.

## 7. AI-specific controls

-   Treat event text and external intelligence as untrusted data, not
    instructions.
-   Do not include secrets in model prompts.
-   Keep model outputs structured and validate them.
-   AI may summarize or recommend; it cannot grant access, alter roles,
    execute shell commands, or automatically contain endpoints.
-   Clearly label generated summaries and link conclusions to
    event/alert IDs.
-   Provide a non-AI fallback if the model is unavailable.
-   Evaluate false positives, false negatives, and prompt-injection
    behavior before release.

## 8. Audit logging

Record actor, action, target, outcome, timestamp, request/correlation
ID, and minimal allowlisted metadata. Never log passwords, tokens,
session cookies, API keys, or unrestricted raw payloads. Restrict access
to audit records and prevent ordinary users from modifying them.

## 9. Safe testing

-   Test only localhost, owned lab machines, and datasets that are
    authorized for use.
-   Use synthetic failed-login and scan-like events rather than
    attacking real services.
-   Do not add autonomous destructive response actions to the MVP.
-   Security tests must not reset databases or modify system-wide
    settings without approval.

## 10. Release checklist

-   [ ] No secrets in repository or images.
-   [ ] Authentication and authorization tests pass.
-   [ ] Tenant-isolation tests pass.
-   [ ] Validation, rate limits, and pagination bounds exist.
-   [ ] Sensitive data is redacted from logs.
-   [ ] Dependency and container checks run where practical.
-   [ ] AI output is treated as untrusted.
-   [ ] Backup and restore steps are tested.
-   [ ] Known limitations are documented.
