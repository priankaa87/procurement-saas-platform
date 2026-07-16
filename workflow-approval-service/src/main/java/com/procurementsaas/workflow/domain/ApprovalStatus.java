package com.procurementsaas.workflow.domain;

/**
 * Approval-request lifecycle.
 *
 * <pre>
 *   PENDING ──every step approved──▶ APPROVED
 *      │ │
 *      │ └──any step rejected──▶ REJECTED
 *      └────withdrawn──────────▶ CANCELLED
 * </pre>
 *
 * A single rejection ends the request: an approval chain is a series of vetoes, not a vote.
 */
public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}
