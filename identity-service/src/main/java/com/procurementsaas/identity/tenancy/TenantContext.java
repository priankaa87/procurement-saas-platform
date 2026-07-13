package com.procurementsaas.identity.tenancy;

/**
 * Holds the current tenant id for the duration of a request (per-thread). Populated by
 * {@link TenantFilter} from the {@code X-Tenant-ID} header (set by the gateway from the
 * JWT {@code tenant} claim) and read by {@link CurrentTenantResolver} to select the schema.
 */
public final class TenantContext {

    public static final String DEFAULT_TENANT = "public";

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenant(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static String getTenant() {
        String t = CURRENT.get();
        return t != null ? t : DEFAULT_TENANT;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
