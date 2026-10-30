import { useEffect, useState, type FormEvent } from "react";
import { Link, useLocation } from "react-router-dom";
import {
  Check,
  ChevronDown,
  Download,
  Eye,
  FileUp,
  Filter,
  Plus,
  RotateCcw,
  Search,
  X,
} from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { api, downloadFile, patch } from "../lib/api";
import { FieldError } from "../components/FieldError";
import { checkFiles, checkFilters, checkReason } from "../lib/validation";
import type {
  Attachment,
  Department,
  Gender,
  Leave,
  LeaveType,
  Page,
} from "../lib/types";
import {
  dateDisplay,
  GENDERS,
  label,
  query,
  STATUSES,
  TYPES,
} from "../lib/types";
import { Alert, Empty, Modal, Pagination, StatusBadge } from "../components/UI";
import { SortHeader, type SortDirection } from "../components/SortHeader";
import {
  persistTableQuery,
  readUrlFilters,
  readUrlNumber,
} from "../lib/tableUrlState";

type LeaveFilters = {
  search: string;
  status: string;
  leaveType: string;
  departmentId: string;
  gender: string;
  employeeId: string;
  startFrom: string;
  startTo: string;
  endFrom: string;
  endTo: string;
  submittedFrom: string;
  submittedTo: string;
  sortBy: string;
  direction: string;
};
const initial: LeaveFilters = {
  search: "",
  status: "",
  leaveType: "",
  departmentId: "",
  gender: "",
  employeeId: "",
  startFrom: "",
  startTo: "",
  endFrom: "",
  endTo: "",
  submittedFrom: "",
  submittedTo: "",
  sortBy: "createdAt",
  direction: "DESC",
};
export function LeavesPage() {
  const { user } = useAuth();
  const location = useLocation();
  const [draft, setDraft] = useState<LeaveFilters>(() =>
    readUrlFilters(initial),
  );
  const [filters, setFilters] = useState<LeaveFilters>(() =>
    readUrlFilters(initial),
  );
  const [page, setPage] = useState(() => readUrlNumber("page", 0));
  const [size, setSize] = useState(() =>
    readUrlNumber("size", 10, [5, 10, 20, 50]),
  );
  const [data, setData] = useState<Page<Leave> | null>(null);
  const [loading, setLoading] = useState(false);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [expanded, setExpanded] = useState(() =>
    [
      "departmentId",
      "gender",
      "employeeId",
      "startFrom",
      "startTo",
      "endFrom",
      "endTo",
      "submittedFrom",
      "submittedTo",
    ].some((k) => new URLSearchParams(window.location.search).has(k)),
  );
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(
    (location.state as { success?: string } | null)?.success ?? "",
  );
  const [refresh, setRefresh] = useState(0);
  const [selected, setSelected] = useState<Leave | null>(null);
  const [documents, setDocuments] = useState<Attachment[]>([]);
  const [docError, setDocError] = useState("");
  const [review, setReview] = useState<"approve" | "reject" | null>(null);
  const [rejectionReason, setRejectionReason] = useState("");
  const [rejectionError, setRejectionError] = useState("");
  const [filterError, setFilterError] = useState("");
  const [busy, setBusy] = useState(false);
  const admin = user?.role === "ADMIN" || user?.role === "SUPER_ADMIN";
  useEffect(() => {
    api<Department[]>("/departments")
      .then(setDepartments)
      .catch(() => {});
  }, []);
  // URL is the durable location for server-query filters, sorting and paging on reload.
  useEffect(() => {
    persistTableQuery({ ...filters, page, size });
  }, [filters, page, size]);
  useEffect(() => {
    if (JSON.stringify(filters) === JSON.stringify(draft)) return;
    const timer = window.setTimeout(() => {
      if (checkFilters(draft)) return;
      setFilters({ ...draft });
      setPage(0);
    }, 350);
    return () => window.clearTimeout(timer);
  }, [draft, filters]);
  useEffect(() => {
    const controller = new AbortController();
    setError("");
    setLoading(true);
    setData(null);
    api<Page<Leave>>("/leaves?" + query({ ...filters, page, size }), {
      signal: controller.signal,
    })
      .then((d) => {
        if (!controller.signal.aborted) setData(d);
      })
      .catch((e) => {
        if (!controller.signal.aborted) setError(e.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [filters, page, size, refresh]);
  useEffect(() => {
    if (!selected) return;
    let active = true;
    setDocError("");
    api<Attachment[]>(`/leaves/${selected.id}/attachments`)
      .then((d) => {
        if (active) setDocuments(d);
      })
      .catch((e) => {
        if (active) setDocError(e.message);
      });
    return () => {
      active = false;
    };
  }, [selected, refresh]);
  const set = (key: keyof LeaveFilters, value: string) => {
    setFilterError("");
    setDraft((s) => ({ ...s, [key]: value }));
  };
  function apply(e?: FormEvent) {
    e?.preventDefault();
    const msg = checkFilters(draft);
    setFilterError(msg);
    if (msg) return;
    setFilters({ ...draft });
    setPage(0);
  }
  function clear() {
    setFilterError("");
    setDraft(initial);
    setFilters(initial);
    setPage(0);
  }
  function sort(field: string) {
    const direction: SortDirection =
      filters.sortBy === field
        ? filters.direction === "ASC"
          ? "DESC"
          : "ASC"
        : "ASC";
    setDraft((f) => ({ ...f, sortBy: field, direction }));
    setFilters((f) => ({ ...f, sortBy: field, direction }));
    setPage(0);
  }
  async function decide() {
    if (!selected || !review) return;
    const msg =
      review === "reject"
        ? checkReason(rejectionReason, "Rejection reason")
        : "";
    setRejectionError(msg);
    if (msg) return;
    setBusy(true);
    setDocError("");
    try {
      if (review === "approve")
        await patch(`/leaves/${selected.id}/approve`, {});
      else
        await patch(`/leaves/${selected.id}/reject`, {
          reason: rejectionReason,
        });
      setSuccess(
        `Leave request #${selected.id} ${review === "approve" ? "approved" : "rejected"} successfully.`,
      );
      setReview(null);
      setSelected(null);
      setRejectionReason("");
      setRefresh((v) => v + 1);
    } catch (ex) {
      setDocError(ex instanceof Error ? ex.message : "Action failed");
    } finally {
      setBusy(false);
    }
  }
  async function upload(file: File) {
    if (!selected) return;
    const msg = checkFiles([file]);
    if (msg) {
      setDocError(msg);
      return;
    }
    setBusy(true);
    setDocError("");
    try {
      const form = new FormData();
      form.append("file", file);
      await api(`/leaves/${selected.id}/attachments`, {
        method: "POST",
        body: form,
      });
      setRefresh((v) => v + 1);
    } catch (ex) {
      setDocError(ex instanceof Error ? ex.message : "Upload failed");
    } finally {
      setBusy(false);
    }
  }
  async function removeDocument(id: number) {
    if (!selected || busy) return;
    if (
      !window.confirm(
        "Remove this supporting document? You can upload a replacement while the request is pending.",
      )
    )
      return;
    setBusy(true);
    setDocError("");
    try {
      await api(`/leaves/${selected.id}/attachments/${id}`, {
        method: "DELETE",
      });
      setRefresh((v) => v + 1);
    } catch (ex) {
      setDocError(ex instanceof Error ? ex.message : "Delete failed");
    } finally {
      setBusy(false);
    }
  }
  const choose = (s: string) => {
    setDraft((d) => ({ ...d, status: s }));
    setFilters((f) => ({ ...f, status: s }));
    setPage(0);
  };
  return (
    <div className="stack gap-lg">
      <div className="page-title-row">
        <div>
          <span className="eyebrow">LEAVE MANAGEMENT</span>
          <h1>{admin ? "Leave requests" : "My leave requests"}</h1>
          <p>
            {admin
              ? "Review submissions, filter records and manage approvals."
              : "Track your requests and manage supporting documents."}
          </p>
        </div>
        {!admin && (
          <Link className="btn btn-primary" to="/leaves/new">
            <Plus size={17} /> Apply for leave
          </Link>
        )}
      </div>
      {error && <Alert message={error} onClose={() => setError("")} />}
      {success && (
        <Alert
          message={success}
          kind="success"
          onClose={() => setSuccess("")}
        />
      )}
      <div className="filter-tabs">
        {["", ...STATUSES].map((s) => (
          <button
            key={s}
            className={`tab ${filters.status === s ? "selected" : ""}`}
            onClick={() => choose(s)}
          >
            {s ? label(s) : "All requests"}
          </button>
        ))}
      </div>
      <div className="panel filters-panel">
        <div className="filter-heading">
          <h2>
            <Filter size={17} /> Filter requests
          </h2>
          <button
            className="btn btn-ghost btn-sm"
            onClick={() => setExpanded((v) => !v)}
          >
            {expanded ? "Hide" : "More"} filters{" "}
            <ChevronDown size={15} className={expanded ? "flip" : ""} />
          </button>
        </div>
        <form
          className="stack gap-sm"
          autoComplete="off"
          noValidate
          onSubmit={apply}
        >
          <div className="filters-grid primary-filters">
            <label>
              Search
              <input
                placeholder="Name, email or reason…"
                maxLength={120}
                autoComplete="off"
                value={draft.search}
                onChange={(e) => set("search", e.target.value)}
              />
            </label>
            <label>
              Status
              <select
                value={draft.status}
                onChange={(e) => set("status", e.target.value)}
              >
                <option value="">All statuses</option>
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {label(s)}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Leave type
              <select
                value={draft.leaveType}
                onChange={(e) => set("leaveType", e.target.value)}
              >
                <option value="">All types</option>
                {TYPES.map((t) => (
                  <option value={t} key={t}>
                    {label(t)}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Sort by
              <select
                value={draft.sortBy}
                onChange={(e) => set("sortBy", e.target.value)}
              >
                <option value="createdAt">Submitted date</option>
                <option value="startDate">Start date</option>
                <option value="endDate">End date</option>
                <option value="status">Status</option>
                <option value="leaveType">Leave type</option>
                <option value="id">Request ID</option>
                <option value="employeeName">Employee name</option>
                <option value="department">Department</option>
                <option value="gender">Gender</option>
                <option value="reason">Reason</option>
              </select>
            </label>
          </div>
          {expanded && (
            <div className="filters-grid extra-filters">
              {admin && (
                <>
                  <label>
                    Department
                    <select
                      value={draft.departmentId}
                      onChange={(e) => set("departmentId", e.target.value)}
                    >
                      <option value="">All departments</option>
                      {departments.map((d) => (
                        <option key={d.id} value={d.id}>
                          {d.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Gender
                    <select
                      value={draft.gender}
                      onChange={(e) => set("gender", e.target.value)}
                    >
                      <option value="">All genders</option>
                      {GENDERS.map((g) => (
                        <option key={g} value={g}>
                          {label(g as Gender)}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Employee ID
                    <input
                      type="number"
                      min="1"
                      step="1"
                      value={draft.employeeId}
                      onChange={(e) => set("employeeId", e.target.value)}
                      placeholder="e.g., 12"
                    />
                  </label>
                </>
              )}
              <label>
                Start date from
                <input
                  type="date"
                  value={draft.startFrom}
                  onChange={(e) => set("startFrom", e.target.value)}
                />
              </label>
              <label>
                Start date to
                <input
                  type="date"
                  value={draft.startTo}
                  min={draft.startFrom}
                  onChange={(e) => set("startTo", e.target.value)}
                />
              </label>
              <label>
                End date from
                <input
                  type="date"
                  value={draft.endFrom}
                  onChange={(e) => set("endFrom", e.target.value)}
                />
              </label>
              <label>
                End date to
                <input
                  type="date"
                  value={draft.endTo}
                  min={draft.endFrom}
                  onChange={(e) => set("endTo", e.target.value)}
                />
              </label>
              <label>
                Submitted from
                <input
                  type="date"
                  value={draft.submittedFrom}
                  onChange={(e) => set("submittedFrom", e.target.value)}
                />
              </label>
              <label>
                Submitted to
                <input
                  type="date"
                  value={draft.submittedTo}
                  min={draft.submittedFrom}
                  onChange={(e) => set("submittedTo", e.target.value)}
                />
              </label>
              <label>
                Sort direction
                <select
                  value={draft.direction}
                  onChange={(e) => set("direction", e.target.value)}
                >
                  <option value="DESC">Newest / Descending</option>
                  <option value="ASC">Oldest / Ascending</option>
                </select>
              </label>
            </div>
          )}
          {filterError && <FieldError message={filterError} />}
          <div className="filter-actions">
            <button className="btn btn-ghost" type="button" onClick={clear}>
              <RotateCcw size={15} /> Clear filters
            </button>
            <button className="btn btn-primary" type="submit">
              <Search size={15} /> Search requests
            </button>
          </div>
        </form>
      </div>

      <div className="panel">
        <div className="panel-header">
          <div>
            <h2>Leave records</h2>
            <p>
              {data?.totalElements ?? "—"} matching request(s) · Server-side
              filtering
            </p>
          </div>
          <label className="table-size">
            Rows{" "}
            <select
              value={size}
              onChange={(e) => {
                setSize(Number(e.target.value));
                setPage(0);
              }}
            >
              <option value={5}>5</option>
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
            </select>
          </label>
        </div>
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <SortHeader
                  label="Request"
                  field="id"
                  sortBy={filters.sortBy}
                  direction={filters.direction as SortDirection}
                  onSort={sort}
                />
                {admin && (
                  <SortHeader
                    label="Employee"
                    field="employeeName"
                    sortBy={filters.sortBy}
                    direction={filters.direction as SortDirection}
                    onSort={sort}
                  />
                )}
                {admin && (
                  <SortHeader
                    label="Department"
                    field="department"
                    sortBy={filters.sortBy}
                    direction={filters.direction as SortDirection}
                    onSort={sort}
                  />
                )}
                {admin && (
                  <SortHeader
                    label="Gender"
                    field="gender"
                    sortBy={filters.sortBy}
                    direction={filters.direction as SortDirection}
                    onSort={sort}
                  />
                )}
                <SortHeader
                  label="Type"
                  field="leaveType"
                  sortBy={filters.sortBy}
                  direction={filters.direction as SortDirection}
                  onSort={sort}
                />
                <SortHeader
                  label="Start date"
                  field="startDate"
                  sortBy={filters.sortBy}
                  direction={filters.direction as SortDirection}
                  onSort={sort}
                />
                <SortHeader
                  label="End date"
                  field="endDate"
                  sortBy={filters.sortBy}
                  direction={filters.direction as SortDirection}
                  onSort={sort}
                />
                <SortHeader
                  label="Submitted"
                  field="createdAt"
                  sortBy={filters.sortBy}
                  direction={filters.direction as SortDirection}
                  onSort={sort}
                />
                <SortHeader
                  label="Status"
                  field="status"
                  sortBy={filters.sortBy}
                  direction={filters.direction as SortDirection}
                  onSort={sort}
                />
                <th>Details</th>
              </tr>
            </thead>
            <tbody>
              {data?.content.map((l) => (
                <tr key={l.id}>
                  <td>
                    <strong className="request-id">
                      #{String(l.id).padStart(4, "0")}
                    </strong>
                  </td>
                  {admin && (
                    <td>
                      <strong>{l.employeeName}</strong>
                      <small className="cell-sub">{l.employeeEmail}</small>
                    </td>
                  )}
                  {admin && <td>{l.department ?? "Unassigned"}</td>}
                  {admin && <td>{label(l.gender)}</td>}
                  <td>
                    <span className="type-pill">{label(l.leaveType)}</span>
                  </td>
                  <td>{dateDisplay(l.startDate)}</td>
                  <td>{dateDisplay(l.endDate)}</td>
                  <td>{dateDisplay(l.createdAt)}</td>
                  <td>
                    <StatusBadge status={l.status} />
                  </td>
                  <td>
                    <button
                      className="btn btn-ghost btn-sm"
                      onClick={() => {
                        setSelected(l);
                        setReview(null);
                        setDocuments([]);
                      }}
                    >
                      <Eye size={15} /> View
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {loading && (
            <div className="table-message">
              Loading matching leave requests from MySQL…
            </div>
          )}
          {!loading && data?.content.length === 0 && <Empty />}
        </div>
        {data && <Pagination page={data} onPage={setPage} />}
      </div>
      {selected && (
        <Modal
          title={`Leave request #${selected.id}`}
          onClose={() => {
            setSelected(null);
            setReview(null);
          }}
          width="640px"
        >
          <div className="stack gap-md">
            {docError && (
              <Alert message={docError} onClose={() => setDocError("")} />
            )}
            <div className="detail-header">
              <div>
                <strong>{selected.employeeName}</strong>
                <small>
                  {selected.employeeEmail} ·{" "}
                  {selected.department ?? "Unassigned"}
                </small>
              </div>
              <StatusBadge status={selected.status} />
            </div>
            <div className="detail-grid">
              <div>
                <span>Leave type</span>
                <strong>{label(selected.leaveType as LeaveType)}</strong>
              </div>
              <div>
                <span>Gender</span>
                <strong>{label(selected.gender as Gender)}</strong>
              </div>
              <div>
                <span>Start date</span>
                <strong>{dateDisplay(selected.startDate)}</strong>
              </div>
              <div>
                <span>End date</span>
                <strong>{dateDisplay(selected.endDate)}</strong>
              </div>
              <div>
                <span>Submitted</span>
                <strong>{dateDisplay(selected.createdAt)}</strong>
              </div>
              <div>
                <span>Reviewed by</span>
                <strong>{selected.reviewer ?? "Not reviewed yet"}</strong>
              </div>
            </div>
            <div className="detail-reason">
              <span>Reason for leave</span>
              <p>{selected.reason}</p>
            </div>
            {selected.rejectionReason && (
              <div className="detail-rejection">
                <strong>Reason for rejection</strong>
                <p>{selected.rejectionReason}</p>
              </div>
            )}
            <div className="doc-header">
              <h3>Supporting documents</h3>
              <span>{documents.length}/3 files</span>
            </div>
            {documents.length === 0 && (
              <p className="muted">No documents attached.</p>
            )}
            {documents.map((d) => (
              <div key={d.id} className="doc-row">
                <span>
                  <FileUp size={16} />
                  <span>
                    <strong>{d.originalName}</strong>
                    <small>{(d.sizeBytes / 1024).toFixed(0)} KB</small>
                  </span>
                </span>
                <span className="doc-controls">
                  <button
                    className="btn btn-sm btn-ghost"
                    onClick={() =>
                      void downloadFile(d.downloadUrl, d.originalName).catch(
                        (e) => setDocError(e.message),
                      )
                    }
                  >
                    <Download size={15} /> Download
                  </button>
                  {!admin && selected.status === "PENDING" && (
                    <button
                      className="btn btn-sm btn-ghost danger-text"
                      disabled={busy}
                      onClick={() => void removeDocument(d.id)}
                      aria-label={`Remove ${d.originalName}`}
                    >
                      <X size={15} /> Remove
                    </button>
                  )}
                </span>
              </div>
            ))}
            {!admin &&
              selected.status === "PENDING" &&
              documents.length < 3 && (
                <label className="btn btn-ghost btn-small-file">
                  <FileUp size={16} /> Add document
                  <input
                    type="file"
                    accept=".pdf,.jpg,.jpeg,.png"
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) void upload(file);
                      e.target.value = "";
                    }}
                  />
                </label>
              )}
            {admin && selected.status === "PENDING" && !review && (
              <div className="form-actions">
                <button
                  className="btn btn-danger-soft"
                  onClick={() => setReview("reject")}
                >
                  <X size={16} /> Reject
                </button>
                <button
                  className="btn btn-success"
                  onClick={() => setReview("approve")}
                >
                  <Check size={16} /> Approve
                </button>
              </div>
            )}
            {review && (
              <div className="review-box">
                <h3>
                  {review === "approve"
                    ? "Approve this request?"
                    : "Reject this request?"}
                </h3>
                <p>
                  {review === "approve"
                    ? "This action cannot be undone."
                    : "Provide a meaningful rejection reason for the employee."}
                </p>
                {review === "reject" && (
                  <label>
                    Rejection reason
                    <textarea
                      placeholder="Explain why the leave request is being rejected…"
                      required
                      minLength={10}
                      maxLength={1000}
                      autoComplete="off"
                      aria-invalid={!!rejectionError}
                      value={rejectionReason}
                      rows={3}
                      onChange={(e) => {
                        setRejectionError("");
                        setRejectionReason(e.target.value);
                      }}
                    />
                    <FieldError message={rejectionError} />
                    <small className="form-help">
                      10–1000 characters · {rejectionReason.trim().length}/1000
                    </small>
                  </label>
                )}
                <div className="form-actions">
                  <button
                    className="btn btn-ghost"
                    onClick={() => setReview(null)}
                  >
                    Cancel
                  </button>
                  <button
                    className="btn btn-primary"
                    disabled={
                      busy ||
                      (review === "reject" &&
                        rejectionReason.trim().length < 10)
                    }
                    onClick={() => void decide()}
                  >
                    {busy ? "Saving…" : "Confirm decision"}
                  </button>
                </div>
              </div>
            )}
          </div>
        </Modal>
      )}
    </div>
  );
}
