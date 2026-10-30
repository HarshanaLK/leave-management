import { useEffect, useState } from "react";
import { Mail, Send } from "lucide-react";
import { api, post } from "../lib/api";
import { useAuth } from "../context/AuthContext";
import { Alert } from "../components/UI";

type MailStatus = {
  enabled: boolean;
  configured: boolean;
  provider: string;
  sender: string;
  smtpHost: string;
};

export function EmailSettingsPage() {
  const { user } = useAuth();
  const [status, setStatus] = useState<MailStatus | null>(null);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    let active = true;
    api<MailStatus>("/notifications/status")
      .then((value) => {
        if (active) setStatus(value);
      })
      .catch((e) => {
        if (active) setError(e.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  async function testEmail() {
    setBusy(true);
    setError("");
    setSuccess("");
    try {
      const result = await post<{ message: string }>(
        "/notifications/test-email",
        {},
      );
      setSuccess(result.message);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not request test email");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="stack gap-lg">
      <div className="page-title-row">
        <div>
          <span className="eyebrow">SUPER ADMIN · NOTIFICATIONS</span>
          <h1>Email settings</h1>

        </div>
      </div>
      {error && <Alert message={error} onClose={() => setError("")} />}
      {success && (
        <Alert
          kind="success"
          message={success}
          onClose={() => setSuccess("")}
        />
      )}
      <div className="panel email-settings-panel">
        <div className="panel-header">
          <div>
            <h2>
              <Mail
                size={19}
                style={{ verticalAlign: "middle", marginRight: 8 }}
              />{" "}
              SMTP connection
            </h2>

          </div>
        </div>
        <div className="email-settings-body stack">
          {loading ? (
            <p>Loading current email configuration…</p>
          ) : (
            status && (
              <>
                <div className="email-status-grid">
                  <div className="email-status-item">
                    <span>Sending</span>
                    <strong>{status.enabled ? "Enabled" : "Disabled"}</strong>
                  </div>
                  <div className="email-status-item">
                    <span>SMTP configuration</span>
                    <strong>
                      {status.configured ? "Ready to test" : "Incomplete"}
                    </strong>
                  </div>
                  <div className="email-status-item">
                    <span>Provider</span>
                    <strong>{status.provider}</strong>
                  </div>
                  <div className="email-status-item">
                    <span>From address</span>
                    <strong>{status.sender}</strong>
                  </div>
                  <div className="email-status-item">
                    <span>SMTP host</span>
                    <strong>{status.smtpHost}</strong>
                  </div>
                  <div className="email-status-item">
                    <span>Test recipient</span>
                    <strong>{user?.email}</strong>
                  </div>
                </div>

                <div className="email-test-row">
                  <div>
                    <strong>Send a test email to yourself</strong>

                  </div>
                  <button
                    className="btn btn-primary"
                    type="button"
                    disabled={!status.enabled || !status.configured || busy}
                    onClick={() => void testEmail()}
                  >
                    <Send size={16} />
                    {busy ? "Queuing…" : "Send test email"}
                  </button>
                </div>
              </>
            )
          )}
        </div>
      </div>
    </div>
  );
}
