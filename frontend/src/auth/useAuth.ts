import { useMemo } from "react";
import { decodeUser, getToken, type AuthUser } from "./auth";

export interface AuthState {
  user: AuthUser | null;
  signedIn: boolean;
  /** True if the user holds the feature code. Mirrors the back end's @PreAuthorize. */
  can: (feature: string) => boolean;
}

/**
 * The app's view of the current user.
 *
 * `can` decides whether to show something. It is intentionally the same question the back
 * end asks before doing something — but the back end's answer is the one that counts, so a
 * user who forces `can` to return true still gets a 403 from the gateway.
 */
export function useAuth(): AuthState {
  return useMemo(() => {
    const user = decodeUser(getToken());
    const features = new Set(user?.features ?? []);
    return {
      user,
      signedIn: user !== null,
      can: (feature: string) => features.has(feature),
    };
  }, []);
}
