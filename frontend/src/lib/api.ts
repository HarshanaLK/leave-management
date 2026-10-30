export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public fields?: Record<string, string>,
  ) {
    super(message);
  }
}
const root = "/api";
export async function api<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const method = (options.method ?? "GET").toUpperCase();
  const headers = new Headers(options.headers);
  if (!(options.body instanceof FormData) && options.body !== undefined)
    headers.set("Content-Type", "application/json");
  if (["POST", "PATCH", "PUT", "DELETE"].includes(method)) {
    const csrfRes = await fetch(`${root}/auth/csrf`, {
      credentials: "same-origin",
    });
    if (!csrfRes.ok)
      throw new ApiError(
        "Could not initialize request security",
        csrfRes.status,
      );
    const csrf: { token: string } = await csrfRes.json();
    headers.set("X-XSRF-TOKEN", csrf.token);
  }
  let response: Response;
  try {
    response = await fetch(root + path, {
      credentials: "same-origin",
      cache: method === "GET" ? "no-store" : "default",
      ...options,
      headers,
    });
  } catch {
    throw new ApiError(
      "Cannot connect to the API. Check that Spring Boot is running.",
      0,
    );
  }
  if (!response.ok) {
    const payload: { error?: string; fields?: Record<string, string> } =
      await response.json().catch(() => ({}));
    const detail = payload.fields
      ? Object.entries(payload.fields)
          .map(([k, v]) => `${k}: ${v}`)
          .join("; ")
      : "";
    throw new ApiError(
      detail || payload.error || `Request failed (${response.status})`,
      response.status,
      payload.fields,
    );
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
export const post = <T>(path: string, data: unknown) =>
  api<T>(path, { method: "POST", body: JSON.stringify(data) });
export const patch = <T>(path: string, data: unknown) =>
  api<T>(path, { method: "PATCH", body: JSON.stringify(data) });
export async function downloadFile(url: string, filename: string) {
  const response = await fetch(url, { credentials: "same-origin" });
  if (!response.ok) throw new ApiError("File download failed", response.status);
  const blob = await response.blob();
  const objectUrl = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = objectUrl;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
}
