// The one place the app learns who the user is and what they may do.
//
// Today the token comes from an env var (development) or nothing (signed out). Wiring
// Keycloak's PKCE flow means changing only `getToken` and `login`/`logout` below; every
// consumer reads through `useAuth`, so nothing else in the app has to know.

export interface AuthUser {
  subject: string;
  username: string;
  tenant: string;
  roles: string[];
  features: string[];
}

const DEV_TOKEN: string | undefined = import.meta.env.VITE_DEV_TOKEN || undefined;

/** The bearer token to send, or null when signed out. */
export function getToken(): string | null {
  return DEV_TOKEN ?? null;
}

/**
 * Decodes the JWT payload without verifying it.
 *
 * Verification is the gateway's job, and duplicating it here would be security theatre —
 * a browser cannot keep a signing key secret. This only reads claims to decide what to
 * show; every action is still authorised server-side.
 */
export function decodeUser(token: string | null): AuthUser | null {
  if (!token) return null;
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  try {
    const payload = JSON.parse(atob(base64UrlToBase64(parts[1])));
    return {
      subject: payload.sub ?? "",
      username: payload.preferred_username ?? payload.sub ?? "unknown",
      tenant: payload.tenant ?? "public",
      roles: rolesFrom(payload),
      features: Array.isArray(payload.features) ? payload.features : [],
    };
  } catch {
    return null;
  }
}

function rolesFrom(payload: Record<string, unknown>): string[] {
  const realm = payload["realm_access"] as { roles?: string[] } | undefined;
  return realm?.roles ?? [];
}

function base64UrlToBase64(value: string): string {
  return value.replace(/-/g, "+").replace(/_/g, "/");
}
