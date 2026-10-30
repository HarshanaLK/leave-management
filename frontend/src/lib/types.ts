export type Gender = "MALE" | "FEMALE" | "OTHER" | "PREFER_NOT_TO_SAY";
export type Role = "SUPER_ADMIN" | "ADMIN" | "EMPLOYEE";
export type Status = "PENDING" | "APPROVED" | "REJECTED";
export type LeaveType = "ANNUAL" | "CASUAL" | "MEDICAL";
export interface User {
  id: number;
  fullName: string;
  email: string;
  gender: Gender;
  role: Role;
  departmentId: number | null;
  department: string | null;
  active: boolean;
  createdAt: string;
}
export interface Department {
  id: number;
  name: string;
}
export interface Leave {
  id: number;
  employeeId: number;
  employeeName: string;
  employeeEmail: string;
  gender: Gender;
  departmentId: number | null;
  department: string | null;
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  reason: string;
  status: Status;
  rejectionReason: string | null;
  reviewer: string | null;
  reviewedAt: string | null;
  createdAt: string;
  version: number;
}
export interface Attachment {
  id: number;
  originalName: string;
  contentType: string;
  sizeBytes: number;
  downloadUrl: string;
}
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
export interface Stats {
  total: number;
  pending: number;
  approved: number;
  rejected: number;
  employees: number;
}
export const GENDERS: Gender[] = [
  "MALE",
  "FEMALE",
  "OTHER",
  "PREFER_NOT_TO_SAY",
];
export const TYPES: LeaveType[] = ["ANNUAL", "CASUAL", "MEDICAL"];
export const STATUSES: Status[] = ["PENDING", "APPROVED", "REJECTED"];
export const label = (value: string) =>
  value
    .split("_")
    .map((w) => w.charAt(0) + w.slice(1).toLowerCase())
    .join(" ");
export const dateDisplay = (v?: string | null) =>
  v
    ? new Date(`${v.slice(0, 10)}T12:00:00`).toLocaleDateString(undefined, {
        month: "short",
        day: "numeric",
        year: "numeric",
      })
    : "—";
export const query = (
  values: Record<string, string | number | boolean | undefined | null>,
) => {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "")
      params.set(key, String(value));
  });
  return params.toString();
};
