package com.procurementsaas.contract.domain;

/**
 * Notice-of-award lifecycle.
 *
 * <pre>
 *   PENDING_ACCEPTANCE ──accept──▶ ACCEPTED ──▶ (work order)
 *          │  │
 *          │  └──decline──▶ DECLINED
 *          └─────lapse────▶ EXPIRED
 * </pre>
 */
public enum AwardStatus {
    /** Issued to the supplier; awaiting their answer. */
    PENDING_ACCEPTANCE,
    /** The supplier accepted; a work order may now be raised. */
    ACCEPTED,
    /** The supplier refused. */
    DECLINED,
    /** The acceptance window closed without an answer. */
    EXPIRED,
    /** Withdrawn by the buyer before acceptance. */
    CANCELLED
}
