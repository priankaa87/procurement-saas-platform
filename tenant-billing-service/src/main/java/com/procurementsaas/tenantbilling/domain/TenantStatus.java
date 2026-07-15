package com.procurementsaas.tenantbilling.domain;

/**
 * Tenant lifecycle.
 *
 * <pre>
 *   PENDING ──provisioned──▶ ACTIVE ⇄ SUSPENDED ──▶ CANCELLED
 * </pre>
 */
public enum TenantStatus {
    /** Registered; schema not yet provisioned. */
    PENDING,
    /** Schema provisioned and the tenant may use the platform. */
    ACTIVE,
    /** Access withdrawn (e.g. non-payment); data retained and reversible. */
    SUSPENDED,
    /** Terminated. */
    CANCELLED
}
