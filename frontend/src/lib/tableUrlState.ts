/**
 * URL persistence for server-query table state.
 * These values are only filters and paging metadata, never credentials or private data.
 * The Spring Boot API is still responsible for applying the actual query to MySQL.
 */
export function readUrlFilters<T extends Record<string, string>>(
  defaults: T,
): T {
  const search = new URLSearchParams(window.location.search);
  return Object.fromEntries(
    Object.entries(defaults).map(([key, fallback]) => [
      key,
      search.get(key) ?? fallback,
    ]),
  ) as T;
}

export function readUrlNumber(
  key: string,
  fallback: number,
  allowed?: readonly number[],
): number {
  const raw = new URLSearchParams(window.location.search).get(key);
  if (raw === null || !/^(0|[1-9]\d*)$/.test(raw)) return fallback;
  const parsed = Number(raw);
  if (!Number.isSafeInteger(parsed) || parsed > 1000000) return fallback;
  if (allowed && !allowed.includes(parsed)) return fallback;
  return parsed;
}

export function readUrlOption<T extends string>(
  key: string,
  fallback: T,
  options: readonly T[],
): T {
  const value = new URLSearchParams(window.location.search).get(
    key,
  ) as T | null;
  return value !== null && options.includes(value) ? value : fallback;
}

export function persistTableQuery(
  state: Record<string, string | number>,
): void {
  const url = new URL(window.location.href);
  // Only the table's own query fields are used by these pages.
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(state)) {
    if (String(value) !== "") params.set(key, String(value));
  }
  const query = params.toString();
  const next = url.pathname + (query ? `?${query}` : "") + url.hash;
  if (next !== url.pathname + url.search + url.hash) {
    window.history.replaceState(window.history.state, "", next);
  }
}
