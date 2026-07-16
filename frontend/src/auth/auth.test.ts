import { describe, expect, it } from "vitest";
import { decodeUser } from "./auth";

/**
 * Unit tests for JWT claim decoding. These pin down that the front end reads the same
 * claim shape the back end issues — realm roles and the custom `features` array — so a
 * change on either side that would silently break the UI's gating shows up here instead.
 */
describe("decodeUser", () => {
  /** Builds an unsigned token with the given payload; signature is irrelevant to decoding. */
  function tokenWith(payload: Record<string, unknown>): string {
    const b64 = (obj: unknown) =>
      btoa(JSON.stringify(obj)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
    return `${b64({ alg: "none" })}.${b64(payload)}.sig`;
  }

  it("pulls username, tenant, roles and features from the claims", () => {
    const user = decodeUser(
      tokenWith({
        sub: "u-1",
        preferred_username: "alice",
        tenant: "acme",
        realm_access: { roles: ["PROCUREMENT_OFFICER"] },
        features: ["FEATURE_TENDER_VIEW", "FEATURE_REPORT_RUN"],
      }),
    );

    expect(user).not.toBeNull();
    expect(user?.username).toBe("alice");
    expect(user?.tenant).toBe("acme");
    expect(user?.roles).toEqual(["PROCUREMENT_OFFICER"]);
    expect(user?.features).toContain("FEATURE_REPORT_RUN");
  });

  it("defaults the tenant to public and roles/features to empty when absent", () => {
    const user = decodeUser(tokenWith({ sub: "u-2" }));
    expect(user?.tenant).toBe("public");
    expect(user?.roles).toEqual([]);
    expect(user?.features).toEqual([]);
  });

  it("returns null for no token, so the app reads as signed out", () => {
    expect(decodeUser(null)).toBeNull();
  });

  it("returns null for a malformed token rather than throwing", () => {
    expect(decodeUser("not-a-jwt")).toBeNull();
    expect(decodeUser("a.b")).toBeNull();
  });
});
