import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import {
  CalendarDays,
  ClipboardList,
  LayoutDashboard,
  LogOut,
  PlusCircle,
  ShieldCheck,
  Users,
  UserCog,
  MailCheck,
} from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { label } from "../lib/types";
export function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  if (!user) return null;
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Link to="/dashboard" className="brand">
          <span className="brand-mark">
            <CalendarDays size={21} />
          </span>
          <span>
            Leave<span className="brand-accent">Hub</span>
            <small>WORKFORCE MANAGEMENT</small>
          </span>
        </Link>
        <div className="nav-label">WORKSPACE</div>
        <nav aria-label="Main navigation" className="nav-links">
          <NavLink to="/dashboard">
            <LayoutDashboard size={18} /> Dashboard
          </NavLink>
          <NavLink to="/leaves" end>
            <ClipboardList size={18} />{" "}
            {user.role !== "EMPLOYEE" ? "Leave requests" : "My leaves"}
          </NavLink>
          {user.role === "EMPLOYEE" && (
            <NavLink to="/leaves/new">
              <PlusCircle size={18} /> Apply for leave
            </NavLink>
          )}
          {user.role !== "EMPLOYEE" && (
            <NavLink to="/employees">
              <Users size={18} /> Employees
            </NavLink>
          )}
          {user.role === "SUPER_ADMIN" && (
            <NavLink to="/admins">
              <UserCog size={18} /> Administrators
            </NavLink>
          )}
          {user.role === "SUPER_ADMIN" && (
            <NavLink to="/email-settings">
              <MailCheck size={18} /> Email settings
            </NavLink>
          )}
        </nav>
        <div className="sidebar-bottom">
          <div className="sidebar-note">
            <ShieldCheck size={17} />
            <span>Role-based access enabled</span>
          </div>
          <div className="sidebar-user">
            <div className="avatar">
              {user.fullName.slice(0, 1).toUpperCase()}
            </div>
            <div className="user-info">
              <strong>{user.fullName}</strong>
              <small>{label(user.role)}</small>
            </div>
          </div>
          <button
            className="logout"
            onClick={() => {
              void logout().then(() => navigate("/login"));
            }}
          >
            <LogOut size={17} /> Sign out
          </button>
        </div>
      </aside>
      <main className="main">
        <div className="topbar">
          <span>Employee Leave Management</span>
          <span className="topbar-right">
            <span className="online-dot" /> {user.email}
          </span>
        </div>
        <div className="page-content">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
