# 06 --- Frontend Specification

## 1. Stack

React, TypeScript, Vite, Tailwind CSS, a maintained router, and a
query/cache library. Choose charting and state libraries only when
needed; avoid redundant state-management tools.

## 2. Design direction

Build a clean analyst console with a dark-first theme, readable severity
indicators, dense but legible tables, consistent spacing, keyboard
navigation, visible focus, and accessible contrast. Avoid decorative
charts that do not support a real analyst task.

## 3. Navigation and pages

### MVP

1.  Login
2.  Overview dashboard
3.  Events search
4.  Alert list
5.  Alert detail
6.  Incident list
7.  Incident workspace
8.  Profile / sign out
9.  Access-denied and not-found states

### Later

-   Asset inventory
-   Detection-rule management
-   Threat intelligence
-   Reports
-   User and role administration
-   Audit log viewer
-   Integration settings
-   AI investigation assistant

## 4. Dashboard requirements

-   Alert counts by severity and status.
-   Recent alerts with links to details.
-   Event volume over a selectable, bounded time range.
-   Detection rule activity.
-   Data freshness and service status.
-   Loading, empty, error, and partial-data states.
-   Use backend data for implemented features; label demo fixtures
    explicitly.

## 5. Alert list and detail

List: - Time, title, severity, status, rule, source/asset, assignment. -
Search and bounded filters. - Pagination and stable sorting. - Bulk
actions only when authorization and audit behavior are defined.

Detail: - Summary and severity. - Detection rule and explanation. -
Evidence and related events. - First/last seen. - Assignment and status
controls. - Link to incident. - Audit/activity timeline.

## 6. Incident workspace

-   Incident summary and lifecycle status.
-   Linked alerts and evidence references.
-   Assignment.
-   Timestamped notes.
-   Activity timeline.
-   Clear confirmation for state-changing operations.
-   Display errors without losing unsaved text.

## 7. Frontend security

-   UI permissions improve usability but do not replace server
    authorization.
-   Avoid storing long-lived credentials in localStorage.
-   Render log content as text; never inject it as trusted HTML.
-   Validate uploaded file metadata on the server.
-   Do not place secrets in frontend environment variables; bundled
    frontend values are public.
-   Do not expose stack traces or internal service details.

## 8. API integration

-   Centralize API client configuration and error handling.
-   Use typed request/response contracts.
-   Handle expired sessions consistently.
-   Cancel stale searches where practical.
-   Avoid polling too aggressively; add realtime updates after REST is
    stable.
-   Add component and end-to-end tests for critical workflows.

## 9. Acceptance criteria

-   Every MVP page works with the real backend.
-   Empty/loading/error states are present.
-   Forms show accessible validation.
-   Critical workflows are keyboard operable.
-   Unauthorized actions are hidden or disabled for usability and still
    rejected by the server.
-   No fake data silently replaces a failed API call.
