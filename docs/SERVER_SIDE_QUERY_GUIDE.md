# Server-side search, filters, sorting and pagination

All directory/leave table rows come from the Spring Boot REST API. The frontend does **not** call `Array.filter()` or `Array.sort()` on a fetched page. It assembles URL parameters from filter controls and sends them to a database-backed API.

## Backend pipeline

1. React builds a query string with `URLSearchParams` (from `frontend/src/lib/types.ts`).
2. The browser sends `GET /api/employees`, `/api/admins` or `/api/leaves` with all nonempty filters, plus `page`, `size`, `sortBy`, and `direction`.
3. The Spring controller validates query params and passes them to its service.
4. The service constructs a JPA `Specification` containing search and filter predicates; it validates the requested sort column against an allowlist.
5. Spring Data executes the SQL `WHERE`, `ORDER BY`, `LIMIT`, and `OFFSET` in MySQL. The `Page` JSON includes **totalElements** and **totalPages** from the backend, not a length calculated by the browser.
6. React displays the rows returned and the server-provided page metadata.

## Test these URLs after signing in

```text
/api/employees?search=maya&gender=FEMALE&sortBy=fullName&direction=ASC&page=0&size=5
/api/employees?departmentId=1&active=true&sortBy=department&direction=DESC&page=0&size=10
/api/admins?search=admin&sortBy=email&direction=ASC&page=0&size=10
/api/leaves?status=PENDING&leaveType=MEDICAL&sortBy=startDate&direction=ASC&page=0&size=5
/api/leaves?search=Doctor&gender=FEMALE&startFrom=2026-10-01&startTo=2026-12-31&page=0&size=10
```

These examples use sample values; actual results depend on your data. The `/api/admins` list requires SUPER_ADMIN and employee records are visible only to admins.

## Verify requests and SQL

1. Open browser DevTools (F12) → **Network** → **Fetch/XHR**.
2. In a table, enter a search term or click a column header. A new `GET /api/...` request should appear (search is debounced ~350 ms).
3. Open its URL: `sortBy` and `direction`, `search`, and filters must match the UI.
4. Inspect the JSON `content`, `number`, `size`, `totalElements`, `totalPages`. The `X-Query-Engine: SPRING_DATA_JPA` and `X-Total-Records` response headers are also included.
5. To log executed SQL **locally only**, start Spring Boot with `debug-sql` profile:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = 'debug-sql'
mvn spring-boot:run
```

Watch the console for Hibernate `select ... where ... order by ... offset ... fetch ...` queries. Avoid this profile with real employee records: bind-variable logs can disclose private names and leave reasons.

## Troubleshooting

- If the table does not update, check whether you are viewing an old Vite tab or still running the previous Spring Boot build. Restart both servers and hard refresh the browser (Ctrl+Shift+R).
- Inspect Network for 400/401/403/500 responses; the frontend displays API errors.
- Filters are combined using AND, except the search term which matches employee name, email, or leave reason using OR. Too many filters can legitimately return zero results.
- Employees can search only their own leaves; department/employee filters cannot bypass that authorization rule.
- Date-range filters work on stored dates. Submitted date comparisons use UTC midnight as implemented in this version.
- Unknown sort columns return HTTP 400 rather than being forwarded to SQL.

## Database-level tests

The `EmployeeRepositoryQueryTest` and `LeaveRepositoryQueryTest` use an H2 in-memory relational database and call the real JPA repositories, exercising SQL filtering, sorting, authorization scoping and pagination. Run `mvn test` from `backend`. For MySQL-specific collation/driver behavior, do an additional local smoke test against MySQL.
