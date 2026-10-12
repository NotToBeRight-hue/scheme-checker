import { ExactJsonBody } from "./exact-json";

export type Resource = Record<string, any>;
export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
    public details: Resource = {},
  ) {
    super(message);
  }
}
let csrf = "";
export async function bootstrapCsrf() {
  const r = await fetch("/api/v1/auth/csrf");
  const d = await r.json();
  if (!r.ok) throw new Error("Unable to initialize secure session");
  csrf = d.csrfToken;
  return Boolean(d.authenticated);
}
export async function api(
  path: string,
  method = "GET",
  body?: unknown,
): Promise<any> {
  if (method !== "GET" && !csrf) await bootstrapCsrf();
  const cookie = document.cookie
    .split("; ")
    .find((x) => x.startsWith("XSRF-TOKEN="));
  const token = cookie ? decodeURIComponent(cookie.substring(11)) : csrf;
  if (method !== "GET")
    window.dispatchEvent(new CustomEvent("api-busy", { detail: 1 }));
  try {
    const r = await fetch("/api/v1" + path, {
      method,
      credentials: "same-origin",
      headers: {
        ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
        ...(method !== "GET" ? { "X-XSRF-TOKEN": token } : {}),
      },
      ...(body !== undefined ? { body: body instanceof ExactJsonBody ? body.json : JSON.stringify(body) } : {}),
    });
    if (r.status === 204) return null;
    const data = await r.json();
    if (!r.ok) {
      if (r.status === 401) window.dispatchEvent(new Event("session-expired"));
      throw new ApiError(
        r.status,
        data.error?.code || "ERROR",
        data.error?.message || "Request failed",
        data.error?.details,
      );
    }
    return data;
  } finally {
    if (method !== "GET")
      window.dispatchEvent(new CustomEvent("api-busy", { detail: -1 }));
  }
}
