import { useEffect, useState, type FormEvent } from "react";
import { Plus, Search, Users, RotateCcw, Pencil } from "lucide-react";
import { api, patch, post, ApiError } from "../lib/api";
import { FieldError } from "../components/FieldError";
import { PasswordField } from "../components/PasswordField";
import {
  checkDepartment,
  checkFilters,
  validationErrors,
  type FieldErrors,
} from "../lib/validation";
import type { Department, Gender, Page, User } from "../lib/types";
import { GENDERS, dateDisplay, label, query } from "../lib/types";
import { Alert, Empty, Modal, Pagination } from "../components/UI";
import { SortHeader, type SortDirection } from "../components/SortHeader";
import {
  persistTableQuery,
  readUrlFilters,
  readUrlNumber,
  readUrlOption,
} from "../lib/tableUrlState";

type Filters = {
  search: string;
  gender: string;
  departmentId: string;
  active: string;
};
const initial: Filters = {
  search: "",
  gender: "",
  departmentId: "",
  active: "",
};
type Form = {
  fullName: string;
  email: string;
  gender: Gender;
  departmentId: string;
  password: string;
  active: boolean;
};
const emptyForm: Form = {
  fullName: "",
  email: "",
  gender: "PREFER_NOT_TO_SAY",
  departmentId: "",
  password: "",
  active: true,
};
export function EmployeesPage() {
  return <TeamPage mode="employees" />;
}
export function AdminsPage() {
  return <TeamPage mode="admins" />;
}
function TeamPage({ mode }: { mode: "employees" | "admins" }) {
  const isAdminPage = mode === "admins";
  const singular = isAdminPage ? "administrator" : "employee";
  const [draft, setDraft] = useState<Filters>(() => readUrlFilters(initial));
  const [filters, setFilters] = useState<Filters>(() =>
    readUrlFilters(initial),
  );
  const [page, setPage] = useState(() => readUrlNumber("page", 0));
  const [size, setSize] = useState(() =>
    readUrlNumber("size", 10, [5, 10, 20, 50]),
  );
  const [sortBy, setSortBy] = useState(() =>
    readUrlOption("sortBy", "createdAt", [
      "id",
      "fullName",
      "email",
      "gender",
      "department",
      "createdAt",
      "active",
    ]),
  );
  const [direction, setDirection] = useState<SortDirection>(() =>
    readUrlOption<SortDirection>("direction", "DESC", ["ASC", "DESC"]),
  );
  const [data, setData] = useState<Page<User> | null>(null);
  const [loading, setLoading] = useState(false);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [busy, setBusy] = useState(false);
  const [editing, setEditing] = useState<User | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState<Form>(emptyForm);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [departmentError, setDepartmentError] = useState("");
  const [departmentName, setDepartmentName] = useState("");
  const [refresh, setRefresh] = useState(0);
  const loadDepartments = () =>
    api<Department[]>("/departments")
      .then(setDepartments)
      .catch((e) => setError(e.message));
  useEffect(() => {
    void loadDepartments();
  }, []);
  // Persist applied query state so browser reloads preserve the current server-side result view.
  useEffect(() => {
    persistTableQuery({ ...filters, page, size, sortBy, direction });
  }, [filters, page, size, sortBy, direction]);
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
    api<Page<User>>(
      `/${mode}?` + query({ ...filters, page, size, sortBy, direction }),
      { signal: controller.signal },
    )
      .then((v) => {
        if (!controller.signal.aborted) setData(v);
      })
      .catch((e) => {
        if (!controller.signal.aborted) setError(e.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [mode, filters, page, size, sortBy, direction, refresh]);
  const set = (key: keyof Filters, v: string) =>
    setDraft((s) => ({ ...s, [key]: v }));
  function open(user?: User) {
    setEditing(user ?? null);
    setForm(
      user
        ? {
            fullName: user.fullName,
            email: user.email,
            gender: user.gender,
            departmentId: String(user.departmentId ?? ""),
            active: user.active,
            password: "",
          }
        : emptyForm,
    );
    setModalOpen(true);
    setError("");
    setFieldErrors({});
    setDepartmentError("");
    setDepartmentName("");
  }
  function sort(field: string) {
    if (field === sortBy) setDirection((d) => (d === "ASC" ? "DESC" : "ASC"));
    else {
      setSortBy(field);
      setDirection("ASC");
    }
    setPage(0);
  }
  async function save(e: FormEvent) {
    e.preventDefault();
    setError("");
    setFieldErrors({});
    const validation = validationErrors(form, !editing);
    if (Object.keys(validation).length) {
      setFieldErrors(validation);
      return;
    }
    setBusy(true);
    try {
      const payload = {
        fullName: form.fullName.trim(),
        email: form.email.trim().toLowerCase(),
        gender: form.gender,
        departmentId: Number(form.departmentId),
        active: form.active,
      };
      if (editing) {
        await patch(`/${mode}/${editing.id}`, payload);
        setSuccess(`${label(singular)} updated successfully.`);
      } else {
        await post(`/${mode}`, { ...payload, password: form.password });
        setSuccess(`${label(singular)} created successfully.`);
      }
      setModalOpen(false);
      setRefresh((n) => n + 1);
    } catch (ex) {
      if (ex instanceof ApiError && ex.fields) setFieldErrors(ex.fields);
      setError(ex instanceof Error ? ex.message : "Save failed");
    } finally {
      setBusy(false);
    }
  }
  async function addDepartment() {
    const name = departmentName.trim();
    setDepartmentError("");
    const validation = checkDepartment(name);
    if (validation) {
      setDepartmentError(validation);
      return;
    }
    setBusy(true);
    setError("");
    try {
      const department = await post<Department>("/departments", { name });
      await loadDepartments();
      setForm((s) => ({ ...s, departmentId: String(department.id) }));
      setDepartmentName("");
      setSuccess(`Department “${department.name}” added.`);
    } catch (ex) {
      setDepartmentError(
        ex instanceof Error ? ex.message : "Could not add department",
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="stack gap-lg">
      <div className="page-title-row">
        <div>
          <span className="eyebrow">
            {isAdminPage ? "ACCESS CONTROL" : "TEAM DIRECTORY"}
          </span>
          <h1>{isAdminPage ? "Administrators" : "Employees"}</h1>
          <p>
            {isAdminPage
              ? "Super administrator: create and manage administrator accounts."
              : "Manage employees, their departments and account status."}
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => open()}>
          <Plus size={17} /> Add {singular}
        </button>
      </div>
      {error && !modalOpen && (
        <Alert message={error} onClose={() => setError("")} />
      )}
      {success && (
        <Alert
          message={success}
          kind="success"
          onClose={() => setSuccess("")}
        />
      )}
      {!isAdminPage && (
        <p className="help-note">
          If an existing employee has an <strong>Unassigned</strong> department,
          click Edit and select the correct department.
        </p>
      )}
      <div className="panel filters-panel">
        <div className="filter-heading">
          <h2>
            <Search size={17} /> Search {mode}
          </h2>
        </div>
        <form
          className="filters-grid employees-filters"
          autoComplete="off"
          onSubmit={(e) => {
            e.preventDefault();
            const v = checkFilters(draft);
            if (v) {
              setError(v);
              return;
            }
            setError("");
            setFilters({ ...draft });
            setPage(0);
          }}
        >
          <label>
            Search name or email
            <input
              placeholder={`Search ${mode}…`}
              maxLength={120}
              autoComplete="off"
              value={draft.search}
              onChange={(e) => set("search", e.target.value)}
            />
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
                  {label(g)}
                </option>
              ))}
            </select>
          </label>
          <label>
            Department
            <select
              value={draft.departmentId}
              onChange={(e) => set("departmentId", e.target.value)}
            >
              <option value="">All departments</option>
              {departments.map((d) => (
                <option value={d.id} key={d.id}>
                  {d.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Account status
            <select
              value={draft.active}
              onChange={(e) => set("active", e.target.value)}
            >
              <option value="">All</option>
              <option value="true">Active</option>
              <option value="false">Inactive</option>
            </select>
          </label>
          <div className="filter-actions">
            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => {
                setDraft(initial);
                setFilters(initial);
                setPage(0);
              }}
            >
              <RotateCcw size={15} /> Reset
            </button>
            <button className="btn btn-primary" type="submit">
              Apply filters
            </button>
          </div>
        </form>
      </div>

      <div className="panel">
        <div className="panel-header">
          <div>
            <h2>{isAdminPage ? "Administrator accounts" : "Team members"}</h2>
            <p>
              {data?.totalElements ?? "—"} matching {mode}
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
                  label="Name"
                  field="fullName"
                  sortBy={sortBy}
                  direction={direction}
                  onSort={sort}
                />
                <SortHeader
                  label="Email"
                  field="email"
                  sortBy={sortBy}
                  direction={direction}
                  onSort={sort}
                />
                <SortHeader
                  label="Gender"
                  field="gender"
                  sortBy={sortBy}
                  direction={direction}
                  onSort={sort}
                />
                <SortHeader
                  label="Department"
                  field="department"
                  sortBy={sortBy}
                  direction={direction}
                  onSort={sort}
                />
                <SortHeader
                  label="Joined"
                  field="createdAt"
                  sortBy={sortBy}
                  direction={direction}
                  onSort={sort}
                />
                <SortHeader
                  label="Status"
                  field="active"
                  sortBy={sortBy}
                  direction={direction}
                  onSort={sort}
                />
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {data?.content.map((user) => (
                <tr key={user.id}>
                  <td>
                    <div className="person-cell">
                      <span className="person-avatar">
                        {user.fullName[0].toUpperCase()}
                      </span>
                      <strong>{user.fullName}</strong>
                    </div>
                  </td>
                  <td>{user.email}</td>
                  <td>{label(user.gender)}</td>
                  <td>
                    {user.department ?? (
                      <span className="text-muted">Unassigned — edit</span>
                    )}
                  </td>
                  <td>{dateDisplay(user.createdAt)}</td>
                  <td>
                    <span
                      className={`status ${user.active ? "status-approved" : "status-rejected"}`}
                    >
                      <span className="dot" />
                      {user.active ? "Active" : "Inactive"}
                    </span>
                  </td>
                  <td>
                    <button
                      className="btn btn-sm btn-ghost"
                      onClick={() => open(user)}
                    >
                      <Pencil size={14} /> Edit
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {loading && (
            <div className="table-message">
              Loading matching records from the backend…
            </div>
          )}
          {!loading && data?.content.length === 0 && (
            <Empty title={`No ${mode} found`} />
          )}
        </div>
        {data && <Pagination page={data} onPage={setPage} />}
      </div>
      {modalOpen && (
        <Modal
          title={editing ? `Edit ${singular}` : `Add ${singular}`}
          onClose={() => setModalOpen(false)}
        >
          <form className="stack" autoComplete="off" noValidate onSubmit={save}>
            {error && <Alert message={error} />}
            <label>
              <span className="field-label">
                Full name <span className="required">*</span>
              </span>
              <input
                required
                minLength={2}
                maxLength={120}
                autoComplete="off"
                placeholder="e.g., Alex Perera"
                aria-invalid={!!fieldErrors.fullName}
                value={form.fullName}
                onChange={(e) => {
                  setFieldErrors((v) => ({ ...v, fullName: "" }));
                  setForm((s) => ({ ...s, fullName: e.target.value }));
                }}
              />
              <FieldError message={fieldErrors.fullName} />
              <small className="form-help">
                2–120 letters, spaces, apostrophes, dots or hyphens.
              </small>
            </label>
            <label>
              <span className="field-label">
                Email address <span className="required">*</span>
              </span>
              <input
                required
                type="email"
                maxLength={190}
                autoComplete="off"
                placeholder="name@company.com"
                aria-invalid={!!fieldErrors.email}
                value={form.email}
                onChange={(e) => {
                  setFieldErrors((v) => ({ ...v, email: "" }));
                  setForm((s) => ({ ...s, email: e.target.value }));
                }}
              />
              <FieldError message={fieldErrors.email} />
            </label>
            <div className="field-grid">
              <label>
                <span className="field-label">
                  Gender <span className="required">*</span>
                </span>
                <select
                  aria-invalid={!!fieldErrors.gender}
                  value={form.gender}
                  onChange={(e) =>
                    setForm((s) => ({ ...s, gender: e.target.value as Gender }))
                  }
                >
                  {GENDERS.map((g) => (
                    <option key={g} value={g}>
                      {label(g)}
                    </option>
                  ))}
                </select>
                <FieldError message={fieldErrors.gender} />
              </label>
              <label>
                <span className="field-label">
                  Department <span className="required">*</span>
                </span>
                <select
                  required
                  aria-invalid={!!fieldErrors.departmentId}
                  value={form.departmentId}
                  onChange={(e) =>
                    setForm((s) => ({ ...s, departmentId: e.target.value }))
                  }
                >
                  <option value="">Choose department</option>
                  {departments.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.name}
                    </option>
                  ))}
                </select>
                <FieldError message={fieldErrors.departmentId} />
              </label>
            </div>
            <div>
              <label>
                <div className="department-add">
                  <input
                    aria-label="New department"
                    autoComplete="off"
                    placeholder="New department name (2–100 characters)"
                    minLength={2}
                    maxLength={100}
                    aria-invalid={!!departmentError}
                    value={departmentName}
                    onChange={(e) => {
                      setDepartmentError("");
                      setDepartmentName(e.target.value);
                    }}
                  />
                  <button
                    type="button"
                    disabled={busy || !departmentName.trim()}
                    className="btn btn-ghost"
                    onClick={() => void addDepartment()}
                  >
                    + Department
                  </button>
                </div>
                <FieldError message={departmentError} />
              </label>
            </div>
            {!editing && (
              <PasswordField
                label="Initial password"
                required
                minLength={8}
                maxLength={72}
                autoComplete="new-password"
                placeholder="Create a strong password"
                value={form.password}
                error={fieldErrors.password}
                help="8–72 characters, uppercase + lowercase + digit + symbol. Share privately."
                onChange={(e) => {
                  setFieldErrors((v) => ({ ...v, password: "" }));
                  setForm((s) => ({ ...s, password: e.target.value }));
                }}
              />
            )}
            {editing && (
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={form.active}
                  onChange={(e) =>
                    setForm((s) => ({ ...s, active: e.target.checked }))
                  }
                />{" "}
                Active account
              </label>
            )}
            <div className="form-actions">
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() => setModalOpen(false)}
              >
                Cancel
              </button>
              <button className="btn btn-primary" disabled={busy}>
                <Users size={15} />
                {busy ? "Saving…" : `Save ${singular}`}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
