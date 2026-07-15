package com.procurementsaas.vendor.domain;

/**
 * Supplier lifecycle states.
 *
 * <pre>
 *   DRAFT ──activate──▶ ACTIVE ──debar──▶ DEBARRED ──reinstate──▶ ACTIVE
 *                         ▲   ──suspend─▶ SUSPENDED ──activate──┘
 *                         └───────────────────────────────────────┘
 * </pre>
 */
public enum SupplierStatus {
    /** Registered but not yet approved to participate. */
    DRAFT,
    /** Approved and eligible to participate in tenders. */
    ACTIVE,
    /** Temporarily blocked; can be reactivated. */
    SUSPENDED,
    /** Blocked by a debarment decision; requires reinstatement. */
    DEBARRED
}
