package com.procurementsaas.enlistment.domain;

/**
 * Enlistment round lifecycle.
 *
 * <pre>
 *   DRAFT ──publish──▶ OPEN ──deadline passes / close──▶ CLOSED ──all decided──▶ COMPLETED
 * </pre>
 */
public enum ScheduleStatus {
    /** Being prepared; criteria can still change. */
    DRAFT,
    /** Suppliers may apply until the deadline. */
    OPEN,
    /** Applications closed; assessment under way. */
    CLOSED,
    /** Every application decided. */
    COMPLETED,
    CANCELLED
}
