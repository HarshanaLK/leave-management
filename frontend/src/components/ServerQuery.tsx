/** Shows the exact endpoint used to fetch this table; verify using browser Network + Spring SQL trace. */
export function ServerQuery({
  path,
  total,
  loading,
}: {
  path: string;
  total: number | undefined;
  loading: boolean;
}) {
  return (
    <div className="server-query" role="status" aria-live="polite">
      <span className="server-query-badge">
        {loading
          ? "Querying server…"
          : total === undefined
            ? "Waiting for backend"
            : "MySQL via Spring Data JPA"}
      </span>
      <details>
        <summary>View backend request</summary>
        <code>GET /api{path}</code>
        <p>
          Filters, sorting and pagination are evaluated by Spring Data JPA
          before this page is returned. In DevTools → Network, inspect this GET
          response and its X-Query-Engine and X-Total-Records headers. This
          request returned {total ?? "…"} matching row(s) across all pages.
        </p>
      </details>
    </div>
  );
}
