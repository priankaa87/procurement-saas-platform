import { getToken } from "../auth/auth";

const API_BASE = import.meta.env.VITE_API_BASE ?? "";

/** A request that failed, carrying the status so callers can distinguish 403 from 404. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

/**
 * The single choke point for talking to the gateway.
 *
 * Every call attaches the bearer token here, so no page has to remember to. A non-2xx
 * response becomes an {@link ApiError} carrying the status — the difference between "you
 * may not" (403) and "it is not there" (404) is exactly what the UI needs to react well.
 */
export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (init.body) headers.set("Content-Type", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);

  const response = await fetch(`${API_BASE}${path}`, { ...init, headers });

  if (!response.ok) {
    const detail = await safeMessage(response);
    throw new ApiError(response.status, detail);
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

async function safeMessage(response: Response): Promise<string> {
  try {
    const body = await response.json();
    return body.message ?? body.error ?? `Request failed (${response.status})`;
  } catch {
    return `Request failed (${response.status})`;
  }
}
