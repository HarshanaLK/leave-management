# QA Review — Employee Leave Management System

Date: 2026-10-29 · Scope: latest non-Docker Spring Boot 3.5 / React / TypeScript / MySQL project.

## Defects corrected in this revision

| ID    | Severity | Finding                                                                                            | Resolution                                                                                                                                            |
| ----- | -------- | -------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| QA-01 | High     | A request with a start date in the past was accepted if its end date was later.                    | `LeaveService.validateDates` now rejects past starts using Asia/Colombo business date; browser `min` and TypeScript validation use the same calendar. |
| QA-02 | Medium   | Selecting a second group of supporting files silently replaced the first group.                    | New selection is merged, duplicates ignored; user can append files up to the quota.                                                                   |
| QA-03 | Medium   | A mistaken supporting file could not be removed from the draft selection.                          | Individual **Remove** and **Clear all** actions on the leave submission form.                                                                         |
| QA-04 | Medium   | An end date could remain earlier than a changed start date.                                        | Clear invalid end selection when start changes; end picker is disabled until start is set.                                                            |
| QA-05 | Medium   | Upload retry could resend files successfully uploaded earlier in a partial failure.                | Keep only failed files and count successful uploads against remaining quota.                                                                          |
| QA-06 | Medium   | Fixed dates in date-policy unit tests would eventually become past dates and make the tests flaky. | Deterministic tests supply a fixed business day to validation.                                                                                        |
| QA-07 | Low      | Medical attachments lacked a documented full life-cycle check.                                     | Added `LeaveAttachmentServiceTest`: pending upload, download, delete, file signatures, size and quota, roles and finalized requests.                  |

## Functional QA matrix

Legend: **Automated** = test code provided; **Code reviewed** = inspected and logic traced, not executed end-to-end; **Manual** = execute on Windows/MySQL before submission.

| Area           | Check                                                                           | Coverage / result                                                                           |
| -------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| Leave dates    | yesterday rejected; same day and future accepted; end before start rejected     | Automated Java + frontend rule tests (frontend check executed in this environment)          |
| Date timezone  | local business date around midnight in Sri Lanka                                | Automated TypeScript/Node QA checks (executed)                                              |
| Leave dates    | hand-edited API requests with past start rejected                               | Backend service code reviewed; integration test recommended                                 |
| Leave dates    | date picker disables previous dates, clears inconsistent range                  | Code reviewed; verify browser behavior manually                                             |
| File upload    | PDF/JPG/PNG only; max 5 MB; maximum 3 files                                     | Automated backend tests added; full Maven run required locally                              |
| File upload    | content signature matches declared extension                                    | Automated backend tests added; full Maven run required locally                              |
| File queue     | multi-select accumulates, duplicate ignored, remove one                         | Automated frontend QA checks executed; confirm in browser                                   |
| File queue     | remove all and choose new documents again                                       | Code reviewed; browser manual                                                               |
| File retry     | failed files only retried; successfully uploaded ones counted                   | Code reviewed; manual network-failure simulation recommended                                |
| File storage   | employees can upload/delete only own pending documents                          | Automated backend tests added; role/access logic reviewed                                   |
| File storage   | administrators can view/download but cannot upload/delete on behalf of employee | Code reviewed; manual role tests recommended                                                |
| Authorization  | EMPLOYEE/ADMIN/SUPER_ADMIN API access                                           | Existing tests and Spring Security annotations reviewed                                     |
| Employee/admin | required fields, gender, department, unique emails                              | Existing frontend and Bean Validation rules reviewed                                        |
| API tables     | backend sort, search, filter, paging; whitelisted sort fields                   | Existing repository tests supplied; prior user log showed query tests passed                |
| Passwords      | reset token expiry and password hash update                                     | Existing persistence/regression tests supplied; prior user log showed password tests passed |
| Email          | welcome, submission, review, reset, test email                                  | Code reviewed; live Gmail/Brevo delivery needs local credentials and inbox testing          |
| Alerts         | success/error auto-dismiss while field errors remain                            | Shared alert component reviewed                                                             |
| Migration      | Flyway V1–V3 preserve existing employee data                                    | Files reviewed; migration against actual database not run here                              |
| Layout         | tablet/mobile breakpoints, keyboard focus, screen readers                       | Static styles reviewed; visual/accessibility browser audit recommended                      |

## Run automated checks (Windows, without Docker)

Backend tests (needs local Maven dependencies):

```powershell
cd backend
mvn clean test
mvn spring-boot:run
```

Frontend tests, type checks and production bundle:

```powershell
cd frontend
npm install
npm run qa:rules
npm run build
npm run dev
```

### Manual regression sequence

1. Log in as EMPLOYEE; create a leave beginning yesterday (should fail in frontend and backend). Create a same-day leave (should work).
2. Choose start November 10, end November 12, then change start to November 15: end should clear and require a new value.
3. Add `medical.pdf`, then `appointment.png`; verify both appear, remove `medical.pdf`, choose it again, and verify only intended files are submitted.
4. Attempt a fourth file, a `.exe`, an empty file and a file larger than 5 MB; verify rejected without submitting unauthorized attachments.
5. Submit pending leave; open My Leaves → View, download document, remove a mistaken uploaded one, add a replacement.
6. Log in as ADMIN; review the attachment, approve leave; as EMPLOYEE try deleting/adding afterward (backend should reject).
7. Test admin searching/filtering/sorting with at least 25 leave records and check Network requests (`GET /api/leaves` query string).
8. Run password reset in a separate InPrivate browser and verify new credentials succeed and old ones fail.
9. Check MySQL records remain after refresh/restart; verify department labels and historical records.
10. Verify email in a real inbox and confirm SMTP errors appear in backend logs if delivery fails.

## Not yet established

The complete Maven suite, React production build, live MySQL migration, SMTP delivery and browser E2E regression testing **have not been executed in this environment**. These require your local Windows software, database and credentials. Do not describe this project as having passed a full end-to-end QA certification until those checks are completed. The application's mail listener logs failed sends rather than retrying persistently, and local-disk attachment/DB operations are not a fully atomic production storage workflow; use an outbox and object storage for production.

## Business-rule decisions

- Past dates are disallowed for **new** leave requests, but historic requests remain visible.
- Today's date is calculated using Asia/Colombo company time, regardless of the browser/device clock time zone. The backend re-checks this on every submission.
- Documents are optional and can be managed by the owner until an administrator makes a decision.
- Approved and rejected leave requests are final; only pending requests permit attachment changes.

## Checks executed during creation of this ZIP

- **19 TypeScript/TSX source files** parsed without syntax diagnostics using TypeScript's transpile API.
- **9 automated frontend business-rule checks** passed with Node.js and TypeScript, including Colombo midnight boundaries, date validation, multi-select and document removal.
- All relative frontend imports resolved to actual source files.
- The ZIP was checked for structural integrity and no real `.env` file was included.
- Java test source was reviewed and eight new attachment-service scenarios plus date-policy cases were added, **but cannot be marked passing until `mvn clean test` runs on your machine**.
