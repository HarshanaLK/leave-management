import { useEffect, useRef, useState, type ReactNode } from "react";
import { ChevronLeft, ChevronRight, X } from "lucide-react";
import type { Page, Status } from "../lib/types";

export function StatusBadge({ status }: { status: Status }) {
  return (
    <span className={`status status-${status.toLowerCase()}`}>
      <span className="dot" />
      {status}
    </span>
  );
}
type AlertProps = {
  message: string;
  kind?: "success" | "error";
  onClose?: () => void;
  /** Defaults: success = 4 seconds, error = 6 seconds. Set to 0 to keep the alert visible. */
  autoDismissMs?: number;
};


export function Alert({
  message,
  onClose,
  kind = "error",
  autoDismissMs,
}: AlertProps) {
  const [visible, setVisible] = useState(true);
  const latestClose = useRef(onClose);
  const duration = autoDismissMs ?? (kind === "success" ? 4_000 : 6_000);


  useEffect(() => {
    latestClose.current = onClose;
  }, [onClose]);

  useEffect(() => {
    setVisible(true);
    if (duration <= 0) return;
    const timer = window.setTimeout(() => {
      setVisible(false);
      latestClose.current?.();
    }, duration);
    return () => window.clearTimeout(timer);
  }, [message, kind, duration]);

  function dismiss() {
    setVisible(false);
    latestClose.current?.();
  }

  if (!visible || !message) return null;
  return (
    <div
      role={kind === "error" ? "alert" : "status"}
      className={`alert ${kind}`}
    >
      <span>{message}</span>
      <button
        type="button"
        className="icon-button"
        aria-label={`Dismiss ${kind} message`}
        title="Dismiss"
        onClick={dismiss}
      >
        <X size={16} />
      </button>
    </div>
  );
}
export function Modal({
  title,
  children,
  onClose,
  width = "520px",
}: {
  title: string;
  children: ReactNode;
  onClose: () => void;
  width?: string;
}) {
  return (
    <div
      className="modal-backdrop"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <section
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-label={title}
        style={{ maxWidth: width }}
      >
        <header className="modal-header">
          <h2>{title}</h2>
          <button
            aria-label="Close dialog"
            className="icon-button"
            onClick={onClose}
          >
            <X size={19} />
          </button>
        </header>
        <div className="modal-body">{children}</div>
      </section>
    </div>
  );
}
export function Pagination<T>({
  page,
  onPage,
}: {
  page: Page<T>;
  onPage: (page: number) => void;
}) {
  return (
    <div className="pagination">
      <span>
        {page.totalElements === 0
          ? "No results"
          : `${page.number * page.size + 1}–${Math.min((page.number + 1) * page.size, page.totalElements)} of ${page.totalElements}`}
      </span>
      <div className="pagination-buttons">
        <button
          className="btn btn-ghost"
          disabled={page.first}
          onClick={() => onPage(page.number - 1)}
        >
          <ChevronLeft size={15} /> Previous
        </button>
        <span>
          Page {page.number + 1} / {Math.max(1, page.totalPages)}
        </span>
        <button
          className="btn btn-ghost"
          disabled={page.last || page.totalPages === 0}
          onClick={() => onPage(page.number + 1)}
        >
          Next <ChevronRight size={15} />
        </button>
      </div>
    </div>
  );
}
export function Empty({
  title = "No results found",
  text = "Try changing your filters.",
}: {
  title?: string;
  text?: string;
}) {
  return (
    <div className="empty">
      <div className="empty-illustration">☷</div>
      <strong>{title}</strong>
      <p>{text}</p>
    </div>
  );
}
