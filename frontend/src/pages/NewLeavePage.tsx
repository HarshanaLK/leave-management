import { useRef, useState, type ChangeEvent, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  CalendarPlus2,
  FileText,
  Info,
  Paperclip,
  Trash2,
  X,
} from "lucide-react";
import { api, post, ApiError } from "../lib/api";
import type { Leave, LeaveType } from "../lib/types";
import { label, TYPES } from "../lib/types";
import { Alert } from "../components/UI";
import { FieldError } from "../components/FieldError";
import {
  checkDates,
  checkFiles,
  checkReason,
  type FieldErrors,
} from "../lib/validation";
import {
  addSelectedDocuments,
  removeSelectedDocument,
  todayInLeaveZone,
  LEAVE_TIME_ZONE,
  MAX_DOCUMENTS,
} from "../lib/leaveRules";

export function NewLeavePage() {
  const navigate = useNavigate();
  const fileInput = useRef<HTMLInputElement>(null);
  const [leaveType, setLeaveType] = useState<LeaveType>("ANNUAL");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [reason, setReason] = useState("");
  const [files, setFiles] = useState<File[]>([]);
  const [uploadedCount, setUploadedCount] = useState(0);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [busy, setBusy] = useState(false);
  const [alreadyCreated, setAlreadyCreated] = useState<number | null>(null);
  const [today] = useState(() => todayInLeaveZone());

  function chooseFiles(event: ChangeEvent<HTMLInputElement>) {
    const incoming = Array.from(event.target.files ?? []);
    // Reset the native control so a removed document can be chosen again.
    event.target.value = "";
    if (!incoming.length) return;
    const next = addSelectedDocuments(files, incoming);
    const issue =
      next.length + uploadedCount > MAX_DOCUMENTS
        ? `Maximum ${MAX_DOCUMENTS} documents per leave request, including ${uploadedCount} already uploaded.`
        : checkFiles(next);
    if (issue) {
      setFieldErrors((v) => ({ ...v, files: issue }));
      return;
    }
    setFiles(next);
    setFieldErrors((v) => ({ ...v, files: "" }));
  }

  function removeFile(index: number) {
    if (busy) return;
    setFiles((current) => removeSelectedDocument(current, index));
    setFieldErrors((v) => ({ ...v, files: "" }));
    if (fileInput.current) fileInput.current.value = "";
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    setError("");
    const validation: FieldErrors = alreadyCreated
      ? {}
      : checkDates(startDate, endDate);
    if (!alreadyCreated) {
      const reasonError = checkReason(reason);
      if (reasonError) validation.reason = reasonError;
    }
    const fileError =
      files.length + uploadedCount > MAX_DOCUMENTS
        ? `Maximum ${MAX_DOCUMENTS} documents per leave request.`
        : checkFiles(files);
    if (fileError) validation.files = fileError;
    setFieldErrors(validation);
    if (Object.keys(validation).length) return;
    setBusy(true);
    try {
      const leave = alreadyCreated
        ? { id: alreadyCreated }
        : await post<Leave>("/leaves", {
            leaveType,
            startDate,
            endDate,
            reason,
          });
      setAlreadyCreated(leave.id);
      const failed: string[] = [];
      const failedFiles: File[] = [];
      let successfulUploads = 0;
      for (const file of files) {
        const payload = new FormData();
        payload.append("file", file);
        try {
          await api(`/leaves/${leave.id}/attachments`, {
            method: "POST",
            body: payload,
          });
          successfulUploads += 1;
        } catch (ex) {
          failedFiles.push(file);
          failed.push(
            `${file.name}: ${ex instanceof Error ? ex.message : "Upload failed"}`,
          );
        }
      }
      setUploadedCount((value) => value + successfulUploads);
      if (failed.length) {
        // Successfully uploaded files must NOT be retried or duplicated.
        setFiles(failedFiles);
        setError(
          `Leave #${leave.id} was created. Some documents could not be uploaded: ${failed.join("; ")}. Remove or retry these files, or manage them from My Leaves.`,
        );
        return;
      }
      navigate("/leaves", {
        state: {
          success: `Leave request #${leave.id} submitted successfully.`,
        },
      });
    } catch (ex) {
      if (ex instanceof ApiError && ex.fields) setFieldErrors(ex.fields);
      setError(
        ex instanceof Error ? ex.message : "Could not create leave request",
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="stack gap-lg narrow-page">
      <div className="page-title-row">
        <div>
          <Link to="/leaves" className="back-link">
            <ArrowLeft size={16} /> Back to my leaves
          </Link>
          <span className="eyebrow">EMPLOYEE PORTAL</span>
          <h1>Apply for leave</h1>
          <p>
            Choose a date today or later, and attach any supporting documents
            before submitting.
          </p>
        </div>
      </div>
      <form
        className="panel form-panel stack"
        autoComplete="off"
        noValidate
        onSubmit={submit}
      >
        <div className="form-section-heading">
          <CalendarPlus2 size={21} />
          <div>
            <h2>Leave details</h2>
            <p>Provide accurate dates and the purpose of your leave.</p>
          </div>
        </div>
        {error && (
          <Alert
            message={error}
            autoDismissMs={alreadyCreated ? 0 : undefined}
            onClose={() => setError("")}
          />
        )}
        {alreadyCreated && (
          <div className="alert success" role="status">
            Leave #{alreadyCreated} is saved. Only pending documents need to be
            uploaded. You can also manage them from My Leaves.
          </div>
        )}
        <label>
          <span className="field-label">
            Leave type <span className="required">*</span>
          </span>
          <select
            value={leaveType}
            disabled={busy || alreadyCreated !== null}
            onChange={(e) => setLeaveType(e.target.value as LeaveType)}
          >
            {TYPES.map((type) => (
              <option key={type} value={type}>
                {label(type)}
              </option>
            ))}
          </select>
        </label>
        <div className="field-grid">
          <label>
            <span className="field-label">
              Start date <span className="required">*</span>
            </span>
            <input
              type="date"
              required
              autoComplete="off"
              min={today}
              value={startDate}
              disabled={busy || alreadyCreated !== null}
              aria-invalid={!!fieldErrors.startDate}
              onChange={(e) => {
                const next = e.target.value;
                setStartDate(next);
                // An earlier chosen end date must never remain invalid silently.
                if (endDate && next && endDate < next) setEndDate("");
                setFieldErrors((v) => ({ ...v, startDate: "", endDate: "" }));
              }}
            />
            <FieldError message={fieldErrors.startDate} />
          </label>
          <label>
            <span className="field-label">
              End date <span className="required">*</span>
            </span>
            <input
              type="date"
              required
              autoComplete="off"
              min={startDate > today ? startDate : today}
              value={endDate}
              disabled={busy || alreadyCreated !== null || !startDate}
              aria-invalid={!!fieldErrors.endDate}
              onChange={(e) => {
                setEndDate(e.target.value);
                setFieldErrors((v) => ({ ...v, endDate: "" }));
              }}
            />
            <FieldError message={fieldErrors.endDate} />
          </label>
        </div>
        <small className="form-help">
          Dates are evaluated using{" "}
          {LEAVE_TIME_ZONE} company time. Same-day leave is allowed.
        </small>
        <label>
          <span className="field-label">
            Reason <span className="required">*</span>
          </span>
          <textarea
            required
            minLength={10}
            maxLength={1000}
            autoComplete="off"
            rows={5}
            aria-invalid={!!fieldErrors.reason}
            placeholder="e.g., Medical appointment at the hospital…"
            value={reason}
            disabled={busy || alreadyCreated !== null}
            onChange={(e) => {
              setReason(e.target.value);
              setFieldErrors((v) => ({ ...v, reason: "" }));
            }}
          />
          <FieldError message={fieldErrors.reason} />
          <small className="form-help">
            10–1000 characters · {reason.trim().length}/1000
          </small>
        </label>
        <div className="upload-section">
          <div className="form-section-heading">
            <Paperclip size={20} />
            <div>
              <h2>Supporting documents (optional)</h2>
              <p>
                Medical reports, appointment letters, or other evidence. You can
                remove a mistaken selection before submitting.
              </p>
            </div>
          </div>
          <label className={`upload-drop ${busy ? "upload-disabled" : ""}`}>
            <FileText size={25} />
            <span>
              <strong>Choose documents</strong> from your device
            </span>
            <small>
              PDF, JPG, PNG · Up to {MAX_DOCUMENTS} files total · 5 MB each
            </small>
            <input
              ref={fileInput}
              type="file"
              accept=".pdf,.jpg,.jpeg,.png"
              multiple
              disabled={busy || files.length + uploadedCount >= MAX_DOCUMENTS}
              onChange={chooseFiles}
              aria-label="Choose supporting documents"
            />
          </label>
          <FieldError message={fieldErrors.files} />
          {files.length > 0 && (
            <div
              className="stack gap-sm"
              aria-label="Selected supporting documents"
            >
              <div className="selected-files-header">
                <strong>
                  Selected documents ({files.length}/{MAX_DOCUMENTS})
                </strong>
                <button
                  type="button"
                  className="btn btn-sm btn-ghost"
                  disabled={busy}
                  onClick={() => {
                    setFiles([]);
                    setFieldErrors((v) => ({ ...v, files: "" }));
                  }}
                >
                  <Trash2 size={14} /> Clear all
                </button>
              </div>
              <div className="selected-document-list">
                {files.map((file, index) => (
                  <div
                    className="selected-document-row"
                    key={`${file.name}-${file.size}-${file.lastModified}`}
                  >
                    <FileText size={17} aria-hidden="true" />
                    <div className="selected-document-details">
                      <strong title={file.name}>{file.name}</strong>
                      <small>{(file.size / 1024).toFixed(1)} KB</small>
                    </div>
                    <button
                      className="btn btn-ghost btn-sm danger-text"
                      type="button"
                      disabled={busy}
                      onClick={() => removeFile(index)}
                      aria-label={`Remove ${file.name} from selection`}
                    >
                      <X size={15} /> Remove
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
        <div className="info-strip">
          <Info size={17} />
          <span>
            Your leave request starts as <strong>Pending</strong>. You can add
            or remove uploaded documents from My Leaves while it is pending.
          </span>
        </div>
        <div className="form-actions">
          <Link className="btn btn-ghost" to="/leaves">
            Cancel
          </Link>
          <button type="submit" disabled={busy} className="btn btn-primary">
            {busy
              ? "Submitting…"
              : alreadyCreated
                ? "Finish / retry document upload"
                : "Submit leave request"}
          </button>
        </div>
      </form>
    </div>
  );
}
