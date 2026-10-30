/** Leave submissions follow the company calendar in Asia/Colombo.
 * Never use toISOString().slice(0, 10) here: it can shift local dates in UTC.
 */
export const LEAVE_TIME_ZONE = "Asia/Colombo";
export const MAX_DOCUMENTS = 3;
export const MAX_DOCUMENT_BYTES = 5 * 1024 * 1024;

export function todayInLeaveZone(date: Date = new Date()): string {
  const parts = new Intl.DateTimeFormat("en-GB", {
    timeZone: LEAVE_TIME_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(date);
  const get = (type: string) =>
    parts.find((part) => part.type === type)?.value ?? "";
  return `${get("year")}-${get("month")}-${get("day")}`;
}

export type DateErrors = Record<"startDate" | "endDate", string>;
export function validateLeaveDates(
  start: string,
  end: string,
  today = todayInLeaveZone(),
): Partial<DateErrors> {
  const errors: Partial<DateErrors> = {};
  if (!start) errors.startDate = "Start date is required.";
  else if (start < today)
    errors.startDate = "Start date cannot be before today.";
  if (!end) errors.endDate = "End date is required.";
  else if (end < today) errors.endDate = "End date cannot be before today.";
  else if (start && end < start)
    errors.endDate = "End date cannot be before the start date.";
  return errors;
}

/** Adds selected files without replacing previous selections. Duplicates are ignored. */
export function addSelectedDocuments(
  current: File[],
  incoming: File[],
): File[] {
  const keys = new Set(
    current.map((f) => `${f.name}::${f.size}::${f.lastModified}`),
  );
  const next = [...current];
  for (const file of incoming) {
    const key = `${file.name}::${file.size}::${file.lastModified}`;
    if (!keys.has(key)) {
      next.push(file);
      keys.add(key);
    }
  }
  return next;
}

export function removeSelectedDocument(current: File[], index: number): File[] {
  return current.filter((_, i) => i !== index);
}
