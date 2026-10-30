import {
  validateLeaveDates,
  MAX_DOCUMENTS,
  MAX_DOCUMENT_BYTES,
} from "./leaveRules";
// Mirror of Spring Bean Validation and LeaveService rules. Backend remains authoritative.
export type FieldErrors = Record<string, string>;
const NAME = /^[\p{L}\p{M}]+(?:[ .'-][\p{L}\p{M}]+)*$/u;
const DEPARTMENT =
  /^[\p{L}\p{M}\p{N}](?:[\p{L}\p{M}\p{N} &()'./-]*[\p{L}\p{M}\p{N})])?$/u;
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PASSWORD =
  /^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9\s])\S{8,72}$/;

export const checkName = (value: string): string => {
  if (!value.trim()) return "Full name is required.";
  if (value.length < 2 || value.length > 120) return "Use 2–120 characters.";
  if (!NAME.test(value))
    return "Use letters, single spaces, dots, apostrophes or hyphens.";
  return "";
};
export const checkEmail = (value: string): string => {
  if (!value.trim()) return "Email address is required.";
  if (value.length > 190 || !EMAIL.test(value))
    return "Enter a valid email address (up to 190 characters).";
  return "";
};
export const checkPassword = (value: string): string => {
  if (!value) return "Password is required.";
  if (!PASSWORD.test(value))
    return "Use 8–72 characters, uppercase, lowercase, a number and a symbol; no spaces.";
  return "";
};
export const checkDepartment = (value: string): string => {
  if (!value.trim()) return "Department name is required.";
  if (value.length < 2 || value.length > 100) return "Use 2–100 characters.";
  if (!DEPARTMENT.test(value))
    return "Use letters, numbers, spaces or basic punctuation.";
  return "";
};
export const checkReason = (value: string, label = "Reason"): string => {
  const length = value.trim().length;
  if (!length) return `${label} is required.`;
  if (length < 10 || value.length > 1000)
    return `${label} must be 10–1000 characters.`;
  return "";
};
export const checkDates = (start: string, end: string): FieldErrors =>
  validateLeaveDates(start, end);
export const checkFiles = (files: File[]): string => {
  if (files.length > MAX_DOCUMENTS)
    return "Maximum three supporting files per request.";
  for (const file of files) {
    if (!file.size || file.size > MAX_DOCUMENT_BYTES)
      return `${file.name}: file must be 1 byte to 5 MB.`;
    if (file.name.length > 255 || !/\.(pdf|jpe?g|png)$/i.test(file.name))
      return `${file.name}: allowed files are PDF, JPG or PNG (max 255-character filename).`;
  }
  return "";
};
export function checkFilters(draft: Record<string, string>): string {
  if ((draft.search?.length ?? 0) > 120)
    return "Search must not exceed 120 characters.";
  if (
    draft.employeeId &&
    (!/^[1-9]\d*$/.test(draft.employeeId) ||
      !Number.isSafeInteger(Number(draft.employeeId)))
  )
    return "Employee ID must be a positive whole number.";
  for (const [start, end] of [
    ["startFrom", "startTo"],
    ["endFrom", "endTo"],
    ["submittedFrom", "submittedTo"],
  ]) {
    if (draft[start] && draft[end] && draft[end] < draft[start])
      return "The end of each date filter must be on or after its beginning.";
  }
  return "";
}
export function validationErrors(
  values: {
    fullName: string;
    email: string;
    gender: string;
    departmentId: string;
    password?: string;
  },
  creating: boolean,
): FieldErrors {
  const result: FieldErrors = {};
  const name = checkName(values.fullName);
  if (name) result.fullName = name;
  const email = checkEmail(values.email);
  if (email) result.email = email;
  if (!values.gender) result.gender = "Select a gender.";
  if (!/^[1-9]\d*$/.test(values.departmentId))
    result.departmentId = "Select a department.";
  if (creating) {
    const password = checkPassword(values.password ?? "");
    if (password) result.password = password;
  }
  return result;
}
