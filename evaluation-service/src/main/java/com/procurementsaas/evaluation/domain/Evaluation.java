package com.procurementsaas.evaluation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The evaluation of one tender's bids.
 *
 * <p>{@code tenderCode} references the Tender service by business code. Technical and
 * financial weights express how the two stages combine and must total 100.
 */
@Entity
@Table(name = "evaluation")
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tender_code", nullable = false, unique = true, length = 50)
    private String tenderCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvaluationStatus status = EvaluationStatus.DRAFT;

    @Column(name = "technical_weight", nullable = false)
    private int technicalWeight;

    @Column(name = "financial_weight", nullable = false)
    private int financialWeight;

    /** Minimum technical score (0–100) required to be considered for financial scoring. */
    @Column(name = "pass_mark", nullable = false, precision = 5, scale = 2)
    private BigDecimal passMark;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Evaluation() {
    }

    public Evaluation(String tenderCode, int technicalWeight, int financialWeight,
                      BigDecimal passMark) {
        if (technicalWeight + financialWeight != 100) {
            throw new IllegalArgumentException(
                "Technical and financial weights must total 100, got "
                    + technicalWeight + " + " + financialWeight);
        }
        if (passMark.compareTo(BigDecimal.ZERO) < 0
            || passMark.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Pass mark must be between 0 and 100");
        }
        this.tenderCode = tenderCode;
        this.technicalWeight = technicalWeight;
        this.financialWeight = financialWeight;
        this.passMark = passMark;
    }

    /** Setup (criteria, participants, technical scores) is only allowed while DRAFT. */
    public boolean isEditable() {
        return status == EvaluationStatus.DRAFT;
    }

    public void closeTechnical() {
        if (status != EvaluationStatus.DRAFT) {
            throw new IllegalStateException(
                "Technical stage is already closed for tender " + tenderCode);
        }
        this.status = EvaluationStatus.TECHNICAL_CLOSED;
    }

    public void complete() {
        if (status != EvaluationStatus.TECHNICAL_CLOSED) {
            throw new IllegalStateException(
                "Technical stage must be closed before financial evaluation for tender "
                    + tenderCode);
        }
        this.status = EvaluationStatus.COMPLETED;
    }

    public Long getId() { return id; }
    public String getTenderCode() { return tenderCode; }
    public EvaluationStatus getStatus() { return status; }
    public int getTechnicalWeight() { return technicalWeight; }
    public int getFinancialWeight() { return financialWeight; }
    public BigDecimal getPassMark() { return passMark; }
    public Instant getCreatedAt() { return createdAt; }
}
