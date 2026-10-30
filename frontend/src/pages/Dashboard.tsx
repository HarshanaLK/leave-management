import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  CalendarCheck2,
  CalendarClock,
  CalendarX2,
  ClipboardList,
  Plus,
  Users,
  ArrowUpRight,
} from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { api } from "../lib/api";
import type { Leave, Page, Stats } from "../lib/types";
import { dateDisplay, label } from "../lib/types";
import { Alert, Empty, StatusBadge } from "../components/UI";
import { SortHeader, type SortDirection } from "../components/SortHeader";

export function Dashboard() {
  const { user } = useAuth();
  const [stats, setStats] = useState<Stats | null>(null);
  const [recent, setRecent] = useState<Leave[]>([]);
  const [sortBy, setSortBy] = useState("createdAt");
  const [direction, setDirection] = useState<SortDirection>("DESC");
  const [error, setError] = useState("");
  useEffect(() => {
    let mounted = true;
    Promise.all([
      api<Stats>("/leaves/stats"),
      api<Page<Leave>>(
        `/leaves?size=5&sortBy=${sortBy}&direction=${direction}`,
      ),
    ])
      .then(([a, b]) => {
        if (mounted) {
          setStats(a);
          setRecent(b.content);
        }
      })
      .catch((e) => {
        if (mounted) setError(e.message);
      });
    return () => {
      mounted = false;
    };
  }, [sortBy, direction]);
  function sort(field: string) {
    if (field === sortBy) setDirection((d) => (d === "ASC" ? "DESC" : "ASC"));
    else {
      setSortBy(field);
      setDirection("ASC");
    }
  }
  const cards = [
    {
      key: "total" as const,
      title: "Total requests",
      icon: ClipboardList,
      cls: "blue",
      desc: "All recorded requests",
    },
    {
      key: "pending" as const,
      title: "Pending review",
      icon: CalendarClock,
      cls: "amber",
      desc: "Awaiting a decision",
    },
    {
      key: "approved" as const,
      title: "Approved leaves",
      icon: CalendarCheck2,
      cls: "green",
      desc: "Successfully approved",
    },
    {
      key: "rejected" as const,
      title: "Rejected leaves",
      icon: CalendarX2,
      cls: "rose",
      desc: "Requests not approved",
    },
  ];
  return (
    <div className="stack gap-lg">
      <div className="page-title-row">
        <div>
          <span className="eyebrow">OVERVIEW</span>
          <h1>
            Good to see you, {user?.fullName.split(" ")[0]}{" "}
            <span className="wave">✳</span>
          </h1>
          <p>Here’s what’s happening with your leave requests.</p>
        </div>
        {user?.role === "EMPLOYEE" && (
          <Link to="/leaves/new" className="btn btn-primary">
            <Plus size={17} /> Apply for leave
          </Link>
        )}
      </div>
      {error && <Alert message={error} />}
      <div className="stat-grid">
        {cards.map((c) => (
          <div className="stat-card" key={c.key}>
            <div className={`stat-icon ${c.cls}`}>
              <c.icon size={21} />
            </div>
            <div className="stat-value">{stats?.[c.key] ?? "—"}</div>
            <div className="stat-label">{c.title}</div>
            <div className="stat-description">{c.desc}</div>
          </div>
        ))}
      </div>
      {user?.role !== "EMPLOYEE" && (
        <div className="notice-banner">
          <div className="notice-icon">
            <Users size={21} />
          </div>
          <div>
            <strong>{stats?.employees ?? "—"} active employees</strong>
            <p>
              Manage your team and review incoming leave requests from one
              place.
            </p>
          </div>
          <Link to="/employees" className="btn btn-white">
            Manage team <ArrowUpRight size={16} />
          </Link>
        </div>
      )}
      <div className="panel">
        <div className="panel-header">
          <div>
            <h2>Recent leave requests</h2>
            <p>Your latest leave activity</p>
          </div>
          <Link to="/leaves" className="text-link">
            View all <ArrowUpRight size={15} />
          </Link>
        </div>
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                {user?.role !== "EMPLOYEE" && (
                  <SortHeader
                    label="Employee"
                    field="employeeName"
                    sortBy={sortBy}
                    direction={direction}
                    onSort={sort}
                  />
                )}
                <SortHeader
                  label="Leave type"
                  field="leaveType"
                  sortBy={sortBy}
                  direction={direction}
                  onSort={sort}
                />
                <SortHeader
                  label="Dates"
                  field="startDate"
                  sortBy={sortBy}
                  direction={direction}
                  onSort={sort}
                />
                <SortHeader
                  label="Submitted"
                  field="createdAt"
                  sortBy={sortBy}
                  direction={direction}
                  onSort={sort}
                />
                <SortHeader
                  label="Status"
                  field="status"
                  sortBy={sortBy}
                  direction={direction}
                  onSort={sort}
                />
              </tr>
            </thead>
            <tbody>
              {recent.map((leave) => (
                <tr key={leave.id}>
                  {user?.role !== "EMPLOYEE" && (
                    <td>
                      <strong>{leave.employeeName}</strong>
                      <small className="cell-sub">
                        {leave.department ?? "Unassigned"}
                      </small>
                    </td>
                  )}
                  <td>
                    <span className="type-pill">{label(leave.leaveType)}</span>
                  </td>
                  <td>
                    {dateDisplay(leave.startDate)} –{" "}
                    {dateDisplay(leave.endDate)}
                  </td>
                  <td>{dateDisplay(leave.createdAt)}</td>
                  <td>
                    <StatusBadge status={leave.status} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!recent.length && (
            <Empty
              title="No leave requests yet"
              text="New requests will appear here."
            />
          )}
        </div>
      </div>
    </div>
  );
}
