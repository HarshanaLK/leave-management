# Password reset persistence and login-session fix

## Why this update exists

The earlier application displayed a successful reset response after invoking its service method, but the frontend left any pre-existing React authentication state untouched. That could make login testing ambiguous: an already-authenticated browser immediately redirects away from the login form without validating a password. The service also relied on Hibernate field-level dirty checking and did not explicitly verify the number of database rows updated.

**These facts do not prove that the database update was failing on your specific machine.** The update makes the database operation explicit and ensures the UI cannot retain an outdated signed-in state after a successful reset.

## What changed

1. `UserRepository.updatePasswordHash` executes a JPQL update against `users`, returns affected row count, and clears the JPA persistence context.
2. `AuthService.reset` BCrypt-hashes the new password, consumes the single-use token, checks that exactly one user record was updated, and reads the account back to verify the stored hash matches the new password. The operation is transactional; an exception rolls everything back rather than returning success.
3. `AuthController.reset` invalidates the **current browser session** after successful commit.
4. `ResetPasswordPage` clears the React auth context and password form fields on success.
5. Tests were added for password-update behavior, token reuse, zero-row updates, and persistence after clearing the JPA context.

## Run without Docker

Preserve your existing database and `.env`. No migration is needed.

From the project root:

```powershell
cd backend
mvn clean test
mvn spring-boot:run
```

In another PowerShell terminal:

```powershell
cd frontend
npm install
npm run dev
```

## Verify the fix

1. In the browser, request **one new reset link** for the exact email address you use to sign in. Old reset links may be expired or consumed.
2. Open the link, enter a strong new password, and submit it. You should see a success response only when the update completes.
3. Open a fresh **Incognito/InPrivate window** at `http://localhost:5173/login` to avoid any session already open in another tab.
4. Sign in with the new password. It should succeed.
5. Sign out. Sign in with the old password. It should now be rejected with HTTP 401.
6. If the old password still works, inspect DevTools > Network. Confirm `/api/auth/login` returns HTTP 200 for the old password, rather than an automatic redirect from an existing session.

For the affected account, inspect MySQL without revealing the password hash:

```sql
USE leave_management;
SELECT id, email, active, updated_at
FROM users
WHERE LOWER(email) = LOWER('employee@example.com');
```

Replace the example email. The `updated_at` field should advance after reset. Confirm your backend uses the same `DB_URL`, `DB_USER`, and database instance for both `/api/auth/reset-password` and `/api/auth/login`.

## Security note

This update logs out the browser performing the reset, **not all sessions on other devices**. Production-wide session revocation would require central session management, e.g. Spring Session with an explicit revoke-all-sessions implementation. Never print password reset tokens, credentials, or complete password hashes in logs or support requests.

## Check locally

`mvn test` includes `PasswordResetServiceTest` and `PasswordHashPersistenceTest`. The complete build and runtime integration could not be executed in the packaging environment, so confirm those tests pass on your machine.
