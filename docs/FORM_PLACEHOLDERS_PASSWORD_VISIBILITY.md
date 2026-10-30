# Form placeholders and show/hide password controls

- Employee/admin creation: placeholders for full name, email, new department, and initial password.
- Authentication: existing login/forgot-email placeholders retained, plus masked-with-toggle password fields on login, new password, and confirmation.
- Leave filtering: employee ID includes a usage example.
- Rejection: free-text example placeholder added.
- Required-field asterisks are now aligned inline with labels in employee/admin and leave submission forms.
- Password inputs default to obscured. The eye button toggles only the selected input, has `type="button"`, and exposes `aria-label`, `title`, and `aria-pressed` states.
- All existing frontend/backend validation remains unchanged.
- Native date selectors and `<select>` elements do not support ordinary text placeholders across browsers; their current labels and select choices remain.
- Browser/password-manager autofill is not guaranteed to obey `autoComplete="off"`; this project does not claim otherwise.
