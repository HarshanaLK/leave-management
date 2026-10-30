# Verification and limitations

## Checks completed when packaging

- Inspected latest provided previous ZIP and traced frontend query parameters through Spring controllers, services, `Specification`s and `PageRequest` repository calls.
- Checked TypeScript syntax/transpilation for 13 frontend source files with TypeScript 5.8.3 (no TS parse errors).
- Parsed `backend/pom.xml` as XML.
- Verified the updated ZIP archive CRC and file listing.
- Confirmed no client-side `.filter()` or `.sort()` calls on employee and leave table response arrays.

## Checks that still need to run on your machine

- `cd backend; mvn test` and `mvn spring-boot:run` with actual MySQL. The package contains H2 repository-level tests but Maven was not available in the packaging environment.
- `cd frontend; npm install; npm run build`. The complete npm install/build was not possible in the packaging environment.
- Browser test: sign in as an administrator and search several known employees, filter by status/department, click headers, and inspect the GET requests.
- Browser test: sign in as an employee and confirm that leave listing cannot expose other employees' records.
- Optional SMTP delivery and medical attachment upload should be validated separately.

## Preserve existing data

- The archive includes the same database migration history (V1, V2, V3) as the previous version; do not delete existing MySQL data or edit migrations that have already run.
- Keep your existing `.env` file when replacing the project directory.
- The additional H2 test dependency is test-only; you still use MySQL for the running application.
