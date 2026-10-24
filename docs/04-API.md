# 04 --- API Contract

## 1. Conventions

-   Base path: `/api/v1`
-   JSON request and response bodies.
-   UTC ISO 8601 timestamps.
-   Consistent pagination with bounded page size.
-   Server-side authorization on every protected route.
-   Tenant scope derived from authenticated context.
-   Never expose stack traces or secrets in errors.
-   Publish an OpenAPI specification and keep it aligned with
    implementation.

## 2. Error format

Use a stable response shape such as:

``` json
{
  "type": "validation_error",
  "title": "Request validation failed",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "requestId": "correlation-id",
  "fieldErrors": [
    {"field": "timestamp", "message": "Must be a valid ISO 8601 timestamp"}
  ]
}
```

Do not return internal exception details to clients.

## 3. Initial endpoint plan

### Authentication

-   `POST /api/v1/auth/login`
-   `POST /api/v1/auth/logout`
-   `POST /api/v1/auth/refresh` only if refresh sessions are implemented
-   `GET /api/v1/auth/me`

Choose a single authentication design and document cookie/token storage,
expiry, rotation, and CSRF considerations before implementation. Do not
store long-lived tokens in browser localStorage by default.

### Events

-   `POST /api/v1/events` --- submit one event
-   `POST /api/v1/events/batch` --- optional bounded batch submission
-   `GET /api/v1/events` --- search with filters and pagination
-   `GET /api/v1/events/{eventId}` --- retrieve an authorized event

### Alerts

-   `GET /api/v1/alerts`
-   `GET /api/v1/alerts/{alertId}`
-   `PATCH /api/v1/alerts/{alertId}` --- update permitted fields such as
    status or assignment
-   `POST /api/v1/alerts/{alertId}/incidents` --- create/link incident

### Incidents

-   `GET /api/v1/incidents`
-   `POST /api/v1/incidents`
-   `GET /api/v1/incidents/{incidentId}`
-   `PATCH /api/v1/incidents/{incidentId}`
-   `POST /api/v1/incidents/{incidentId}/notes`
-   `GET /api/v1/incidents/{incidentId}/timeline`

### Administration

-   `GET /api/v1/users`
-   `POST /api/v1/users`
-   `PATCH /api/v1/users/{userId}`
-   `GET /api/v1/audit-logs`

These are planned contracts, not claims that the routes already exist.

## 4. Event request example

``` json
{
  "timestamp": "2026-10-12T12:00:00Z",
  "sourceType": "linux",
  "sourceName": "lab-host-01",
  "eventType": "authentication_failure",
  "host": "lab-host-01",
  "sourceIp": "192.0.2.25",
  "username": "demo-user",
  "message": "Authentication failure in authorized lab"
}
```

The API derives tenant scope from the authenticated user or ingestion
credential. Client-supplied tenant IDs must not override that scope.

## 5. Query and pagination rules

-   Use allowlisted sort fields.
-   Enforce maximum page size.
-   Require a bounded time range for expensive event searches.
-   Validate IPs and timestamps.
-   Return a stable cursor or page contract.
-   Do not allow user input to become a raw database query or search DSL
    fragment.

## 6. Realtime updates

Add WebSocket or Server-Sent Events only after REST flows work.
Authenticate the connection, authorize each subscription, scope messages
by tenant, and test that clients cannot subscribe to another tenant's
events.

## 7. API acceptance criteria

-   OpenAPI document validates.
-   Valid requests return documented response codes.
-   Invalid input returns safe structured errors.
-   Unauthenticated requests are rejected.
-   Cross-tenant access is denied and tested.
-   Pagination and filters have explicit limits.
-   Integration tests cover success and failure cases.
