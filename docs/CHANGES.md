# Revision details

- V1 unchanged for Flyway checksum integrity.
- V2 repairs VARCHAR(64) token hash mismatch.
- V3 adds transparent Unassigned label for preexisting employee accounts whose department_id was NULL. Review and assign the correct department via Employees → Edit.
- SUPER_ADMIN manages ADMIN accounts; admin and super-admin review requests; permission checks in controllers and service.
- Adds backend sort whitelist and clickable data-column headers on employee, admin, leave and dashboard views.
- Removes Docker Compose as a runtime dependency. Startup is native Windows MySQL + Java + React.
- Corrects nested `/leaves/new` navigation matching.

- Uses JPA/Hibernate entity accessors when reading lazy department/employee/reviewer relations; avoids missing department names and phantom nulls from proxy field access. Password reset now loads and updates a managed user entity.

## Query-visibility update

- Browser search and filters now automatically trigger fresh backend GET requests (350 ms debounce) rather than relying on an invisible Apply step.
- Each table includes a collapsible backend URL indicator showing the exact current query.
- GET table responses carry non-cacheable headers and server result count.
- Refresh cancels older overlapping requests to avoid stale rows appearing during rapid filtering.
- H2 repository integration tests prove dynamic JPA specifications and pageable sorting happen before the data reaches React.
- Local-only SQL debug profile for direct verification of SELECT/WHERE/ORDER BY/LIMIT.

## Free SMTP email integration (Oct 2026)

- Enabled Brevo SMTP delivery with multipart HTML + plain text emails, safely escaped dynamic values and direct recovery links.
- Existing employee and admin emails now use templates for welcome, leave events, approvals, rejections and password resets.
- Super Admin-only Email settings UI and test action (sends only to the logged-in Super Admin).
- Backend mail status endpoint reveals readiness but not SMTP credentials or token contents.
- See docs/EMAIL_SETUP.md; native Windows setup without Docker. No database schema changes.
