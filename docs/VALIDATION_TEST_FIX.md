# Maven validation test fix (2026-10-29)

## Symptom

`mvn clean test` failed at `InputValidationTest.validatesLeaveAndRejectionText` line 35.

## Root cause

`@Size(min=10)` counts whitespace. `"  short   "` therefore passes the DTO check even though `LeaveService.requireTextLength()` rejects it after `.strip()`.

## Fix

- Added `@TrimmedLength` and its `ConstraintValidator` to validate post-trimming length in both leave-create and leave-reject DTOs.
- Kept `@Size(max=1000)` to enforce the same raw form limit as the React UI.
- Made persisted leave/rejection reasons use `.strip()` consistently.
- Extended `InputValidationTest` with padded-short, whitespace-only, valid, and too-long cases.

No MySQL migrations or Docker are needed. Existing data is unchanged.

## Validate locally

From the backend folder:

```powershell
mvn clean test
mvn spring-boot:run
```

This change was reviewed structurally, but a full Maven test run was not available in the packaging environment. Please report any subsequent failing test with the assertion and stack trace.
