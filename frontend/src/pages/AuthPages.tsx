import { useState, type FormEvent, type ReactNode } from "react";
import { Link, Navigate, useNavigate, useSearchParams } from "react-router-dom";
import {
  ArrowRight,
  CalendarDays,
  LockKeyhole,
  Mail,
  ShieldCheck,
} from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { Alert } from "../components/UI";
import { post, ApiError } from "../lib/api";
import { FieldError } from "../components/FieldError";
import { PasswordField } from "../components/PasswordField";
import { checkEmail, checkPassword, type FieldErrors } from "../lib/validation";

function AuthFrame({
  children,
  subtitle,
}: {
  children: ReactNode;
  subtitle: string;
}) {
  return (
    <div className="auth-layout">
      <div className="auth-side">
        <div className="auth-side-inner">
          <div className="auth-brand">
            <CalendarDays /> LeaveHub
          </div>
          <div className="auth-promo">
            <div className="auth-spark">✦ ORGANIZED WORK. BETTER TEAMS.</div>
            <h1>
              Leave management,
              <br />
              <em>made effortless.</em>
            </h1>
            <p>
              A clear and simple way for teams to manage leave requests, stay
              connected and make decisions.
            </p>
          </div>
          <span className="auth-footer">
            Employee Leave Management System · Omibits Assignment
          </span>
        </div>
      </div>
      <div className="auth-form-area">
        <div className="auth-form-card">
          <div className="auth-mobile-brand">
            <CalendarDays /> LeaveHub
          </div>
          <div className="auth-icon">
            <LockKeyhole size={22} />
          </div>
          <h2>{subtitle}</h2>
          {children}
        </div>
      </div>
    </div>
  );
}
export function LoginPage() {
  const { user, login } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [fields, setFields] = useState<FieldErrors>({});
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();
  if (user) return <Navigate to="/dashboard" replace />;
  async function submit(e: FormEvent) {
    e.preventDefault();
    setError("");
    const errors: FieldErrors = {};
    const ev = checkEmail(email);
    if (ev) errors.email = ev;
    if (!password) errors.password = "Password is required.";
    if (password.length > 128) errors.password = "Password is too long.";
    setFields(errors);
    if (Object.keys(errors).length) return;
    setBusy(true);
    try {
      await login(email.trim(), password);
      navigate("/dashboard");
    } catch (ex) {
      if (ex instanceof ApiError && ex.fields) setFields(ex.fields);
      setError(ex instanceof Error ? ex.message : "Login failed");
    } finally {
      setBusy(false);
    }
  }
  return (
    <AuthFrame subtitle="Welcome back">
      <p className="auth-desc">
        Enter your credentials to access your workspace.
      </p>
      <form className="stack" autoComplete="off" noValidate onSubmit={submit}>
        {error && <Alert message={error} />}
        <label>
          Email address
          <input
            type="email"
            required
            maxLength={190}
            autoComplete="off"
            placeholder="you@company.com"
            aria-invalid={!!fields.email}
            value={email}
            onChange={(e) => {
              setFields((v) => ({ ...v, email: "" }));
              setEmail(e.target.value);
            }}
          />
          <FieldError message={fields.email} />
        </label>
        <PasswordField
          label="Password"
          required
          maxLength={128}
          autoComplete="off"
          placeholder="Enter your account password"
          value={password}
          error={fields.password}
          onChange={(e) => {
            setFields((v) => ({ ...v, password: "" }));
            setPassword(e.target.value);
          }}
        />
        <Link className="text-link right" to="/forgot-password">
          Forgot password?
        </Link>
        <button className="btn btn-primary btn-full" disabled={busy}>
          {busy ? "Signing in…" : "Sign in"} <ArrowRight size={17} />
        </button>
      </form>
      <div className="auth-security">
        <ShieldCheck size={15} /> Secure employee and administrator portal
      </div>
    </AuthFrame>
  );
}
export function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [error, setError] = useState("");
  const [emailError, setEmailError] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  async function submit(e: FormEvent) {
    e.preventDefault();
    setError("");
    const msg = checkEmail(email);
    setEmailError(msg);
    if (msg) return;
    setBusy(true);
    try {
      const v = await post<{ message: string }>("/auth/forgot-password", {
        email: email.trim(),
      });
      setMessage(v.message);
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : "Unable to submit");
    } finally {
      setBusy(false);
    }
  }
  return (
    <AuthFrame subtitle="Reset your password">
      <p className="auth-desc">
        We’ll email a one-time reset link if your account exists.
      </p>
      <form className="stack" autoComplete="off" noValidate onSubmit={submit}>
        {error && <Alert message={error} />}{" "}
        {message && <Alert message={message} kind="success" />}
        <label>
          Work email
          <input
            type="email"
            required
            maxLength={190}
            autoComplete="off"
            aria-invalid={!!emailError}
            value={email}
            onChange={(e) => {
              setEmailError("");
              setEmail(e.target.value);
            }}
            placeholder="you@company.com"
          />
          <FieldError message={emailError} />
        </label>
        <button className="btn btn-primary btn-full" disabled={busy}>
          <Mail size={17} /> Send reset link
        </button>
        <Link className="text-link" to="/login">
          ← Back to sign in
        </Link>
      </form>
    </AuthFrame>
  );
}
export function ResetPasswordPage() {
  const { clearSession } = useAuth();
  const [params] = useSearchParams();
  const token = params.get("token") ?? "";
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState("");
  const [fields, setFields] = useState<FieldErrors>({});
  const [done, setDone] = useState(false);
  const [busy, setBusy] = useState(false);
  async function submit(e: FormEvent) {
    e.preventDefault();
    setError("");
    const errors: FieldErrors = {};
    const msg = checkPassword(password);
    if (msg) errors.newPassword = msg;
    if (password !== confirm) errors.confirm = "Passwords do not match.";
    if (!confirm) errors.confirm = "Confirm your password.";
    setFields(errors);
    if (Object.keys(errors).length) return;
    setBusy(true);
    try {
      await post("/auth/reset-password", { token, newPassword: password });
      clearSession();
      setPassword("");
      setConfirm("");
      setDone(true);
    } catch (ex) {
      if (ex instanceof ApiError && ex.fields) setFields(ex.fields);
      setError(ex instanceof Error ? ex.message : "Reset failed");
    } finally {
      setBusy(false);
    }
  }
  return (
    <AuthFrame subtitle="Choose a new password">
      {done ? (
        <div className="stack">
          <Alert
            kind="success"
            message="Password updated. You can sign in now."
          />
          <Link className="btn btn-primary" to="/login">
            Back to sign in
          </Link>
        </div>
      ) : (
        <form className="stack" autoComplete="off" noValidate onSubmit={submit}>
          {error && <Alert message={error} />}{" "}
          {!token && <Alert message="Missing password reset token" />}
          <PasswordField
            label="New password"
            required
            autoComplete="new-password"
            minLength={8}
            maxLength={72}
            placeholder="Create your new password"
            value={password}
            error={fields.newPassword}
            help="8–72 characters including uppercase, lowercase, number and symbol; no spaces."
            onChange={(e) => {
              setFields((v) => ({ ...v, newPassword: "" }));
              setPassword(e.target.value);
            }}
          />
          <PasswordField
            label="Confirm password"
            required
            autoComplete="new-password"
            minLength={8}
            maxLength={72}
            placeholder="Re-enter your new password"
            value={confirm}
            error={fields.confirm}
            onChange={(e) => {
              setFields((v) => ({ ...v, confirm: "" }));
              setConfirm(e.target.value);
            }}
          />
          <button
            className="btn btn-primary btn-full"
            disabled={!token || busy}
          >
            Update password
          </button>
        </form>
      )}
    </AuthFrame>
  );
}
