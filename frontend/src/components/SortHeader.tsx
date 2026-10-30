import { ArrowDown, ArrowUp, ArrowUpDown } from "lucide-react";
export type SortDirection = "ASC" | "DESC";
export function SortHeader({
  label,
  field,
  sortBy,
  direction,
  onSort,
}: {
  label: string;
  field: string;
  sortBy: string;
  direction: SortDirection;
  onSort: (field: string) => void;
}) {
  const active = sortBy === field;
  const Icon = active
    ? direction === "ASC"
      ? ArrowUp
      : ArrowDown
    : ArrowUpDown;
  return (
    <th
      aria-sort={
        active ? (direction === "ASC" ? "ascending" : "descending") : "none"
      }
    >
      <button
        type="button"
        className={`table-sort ${active ? "sorted" : ""}`}
        onClick={() => onSort(field)}
        title={`Sort by ${label}`}
        aria-label={`Sort by ${label}${active ? ` (${direction.toLowerCase()})` : ""}`}
      >
        {label}
        <Icon size={14} aria-hidden="true" />
      </button>
    </th>
  );
}
