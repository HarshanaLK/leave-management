# Validation checklist

All forms reject invalid values client-side and also on the server where applicable.

1. Add employee and add admin: empty/one-letter/number-containing names, badly formed/overlong emails, missing department, invalid password, valid international letters.
2. Add department: empty, one character, duplicate, invalid punctuation, over 100.
3. Login and forgot password: empty or malformed email. Login does not require password strength so existing accounts remain usable.
4. Reset password: mismatch, weak, empty, 73-character password, reused or expired token.
5. New leave: missing dates, end before start, reason too short/long, 4 attachments, bad extension, oversized file, invalid actual file contents.
6. Rejection: empty, 9-character or whitespace padded reason, valid 10–1000-character reason.
7. Search: overlong terms and invalid employee ID/date ranges should not trigger backend requests. Backend independently rejects invalid parameters.
8. API: POST malformed DTO and inspect HTTP 400 `fields` map; do not rely on HTML validation.

Autofill hints can be overridden by third-party password managers.
