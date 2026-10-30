# Free transactional email: Brevo SMTP (no Docker)

LeaveHub sends plain-text and HTML transactional messages through JavaMailSender. **Email is off by default**. You need your own free Brevo account, approved sender, and SMTP credentials to deliver external email. There is no API secret in this repository.

## Brevo setup

1. Sign up at https://www.brevo.com/ and activate transactional sending, if account approval is required.
2. Add and verify a sender email/domain in Brevo. A sender on your own authenticated domain is recommended for deliverability.
3. Open **Settings → SMTP & API → SMTP** and generate an SMTP key. Copy the **SMTP login** exactly as displayed: it may differ from your account email. Do **not** use a Brevo API key for SMTP.
4. In the **root** `.env` file (one level above `backend`), set:

```dotenv
MAIL_ENABLED=true
MAIL_FROM=verified@example.com
SMTP_HOST=smtp-relay.brevo.com
SMTP_PORT=587
SMTP_USERNAME=brevo_smtp_login
SMTP_PASSWORD=brevo_smtp_key
SMTP_AUTH=true
SMTP_STARTTLS=true
```

5. Restart Spring Boot: in `backend`, run `mvn spring-boot:run`. React runs separately via `npm run dev` in `frontend`.
6. Log in as **SUPER_ADMIN → Email settings → Send test email**. The recipient is your _own logged-in super-admin account_; the endpoint does not accept an arbitrary recipient. Check inbox/spam and Spring Boot logs.
7. If an API response says **queued**, that only means the event was published, **not** that the email was delivered. The backend logs SMTP acceptance or failure. Brevo's delivery reports can provide additional status.

The provider's published free tier allows **300 sends/day** (limits and conditions may change): https://help.brevo.com/hc/en-us/articles/208580669-FAQs-What-are-the-limits-of-the-Free-plan

## Events included

| Event                        | Recipient                                                              |
| ---------------------------- | ---------------------------------------------------------------------- |
| Employee created             | New employee: welcome + sign-in link; initial password is **not** sent |
| Administrator created        | New administrator: welcome + sign-in link                              |
| Employee applies for leave   | Employee: receipt; each active admin/super-admin: review notification  |
| Admin approves leave         | Employee: approval with dates                                          |
| Admin rejects leave          | Employee: rejection with reason                                        |
| Password reset requested     | Account owner: one-time 30-minute recovery link                        |
| Password changed             | Account owner: security notice                                         |
| Super-admin uses test button | Their own account email                                                |

Email failure **does not roll back the business transaction**: delivery is triggered after database commit using `@TransactionalEventListener(AFTER_COMMIT)` and `@Async`. Logging intentionally excludes email bodies and reset-token URLs. There is no durable retry queue; for production, add a transactional outbox + retry worker.

## Common errors

- `535 Authentication failed`: use the exact **SMTP login** and **SMTP key**, not account password/API key.
- Timeout: run `Test-NetConnection smtp-relay.brevo.com -Port 587` in PowerShell; check firewall/network policies.
- No message sent: check `MAIL_ENABLED=true`; start backend in `backend` so the relative `.env` import resolves.
- Sender rejected/not delivered: verify sender/domain and activate transactional sending in Brevo.
- Daily quota reached: try again after your provider quota resets; sending to an employee and admins uses one email per recipient.
- `MAIL_ENABLED=false`: email notifications are intentionally skipped; password reset email cannot be used.

## Local alternative

You can test without a live provider by running a locally installed SMTP catcher such as **Mailpit** on Windows and configuring `MAIL_ENABLED=true`, `SMTP_HOST=localhost`, `SMTP_PORT=1025`, `SMTP_AUTH=false`, `SMTP_STARTTLS=false`. It captures emails but **does not send real email to external inboxes**. No Docker is required when Mailpit is installed as a local executable.

## API

Both endpoints require an authenticated **SUPER_ADMIN** session. `POST` also requires CSRF protection (automatically handled by the React API client).

- `GET /api/notifications/status`: `{ enabled, configured, sender, provider, smtpHost }`. SMTP secrets are never returned.
- `POST /api/notifications/test-email`: HTTP `202 Accepted` means queued, not proven delivered. Requires `MAIL_ENABLED=true` and sufficiently complete SMTP config.
