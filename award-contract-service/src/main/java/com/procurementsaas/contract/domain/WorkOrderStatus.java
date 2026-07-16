package com.procurementsaas.contract.domain;

/**
 * Work-order lifecycle.
 *
 * <pre>
 *   DRAFT ──issue──▶ ISSUED ──first receipt──▶ IN_PROGRESS ──all lines delivered──▶ COMPLETED
 * </pre>
 */
public enum WorkOrderStatus {
    /** Being prepared; lines can still be added. */
    DRAFT,
    /** Sent to the supplier; the schedule is fixed. */
    ISSUED,
    /** Some goods have been received. */
    IN_PROGRESS,
    /** Every line fully delivered. */
    COMPLETED,
    CANCELLED
}
