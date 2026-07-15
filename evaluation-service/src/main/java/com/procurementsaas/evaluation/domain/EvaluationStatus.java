package com.procurementsaas.evaluation.domain;

/**
 * Evaluation stages. The order is the point: technical judgement is closed before any
 * price is scored, so cost cannot colour the technical assessment.
 *
 * <pre>
 *   DRAFT ──closeTechnical──▶ TECHNICAL_CLOSED ──complete──▶ COMPLETED
 * </pre>
 */
public enum EvaluationStatus {
    /** Criteria, participants, and technical scores are being captured. */
    DRAFT,
    /** Technical scoring is final; qualification decided. Financial scoring may begin. */
    TECHNICAL_CLOSED,
    /** Financial scoring done, combined scores ranked, comparative statement available. */
    COMPLETED
}
