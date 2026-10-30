import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider, useAuth } from "./context/AuthContext";
import { Layout } from "./components/Layout";
import {
  LoginPage,
  ForgotPasswordPage,
  ResetPasswordPage,
} from "./pages/AuthPages";
import { Dashboard } from "./pages/Dashboard";
import { LeavesPage } from "./pages/LeavesPage";
import { NewLeavePage } from "./pages/NewLeavePage";
import { EmployeesPage, AdminsPage } from "./pages/EmployeesPage";
import { EmailSettingsPage } from "./pages/EmailSettingsPage";
import "./styles.css";

function Protected() {
  const { user, loading } = useAuth();
  if (loading) return <div className="app-loading">Loading LeaveHub…</div>;
  if (!user) return <Navigate to="/login" replace />;
  return <Layout />;
}
function EmployeeOnly({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  return user?.role === "EMPLOYEE" ? (
    <>{children}</>
  ) : (
    <Navigate to="/dashboard" replace />
  );
}
function AdminOnly({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  return user?.role === "ADMIN" || user?.role === "SUPER_ADMIN" ? (
    <>{children}</>
  ) : (
    <Navigate to="/dashboard" replace />
  );
}
function SuperAdminOnly({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  return user?.role === "SUPER_ADMIN" ? (
    <>{children}</>
  ) : (
    <Navigate to="/dashboard" replace />
  );
}
function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route element={<Protected />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/leaves" element={<LeavesPage />} />
            <Route
              path="/leaves/new"
              element={
                <EmployeeOnly>
                  <NewLeavePage />
                </EmployeeOnly>
              }
            />
            <Route
              path="/employees"
              element={
                <AdminOnly>
                  <EmployeesPage />
                </AdminOnly>
              }
            />
            <Route
              path="/admins"
              element={
                <SuperAdminOnly>
                  <AdminsPage />
                </SuperAdminOnly>
              }
            />
            <Route
              path="/email-settings"
              element={
                <SuperAdminOnly>
                  <EmailSettingsPage />
                </SuperAdminOnly>
              }
            />
          </Route>
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
