package com.procurementsaas.evaluation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/**
 * One supplier's standing in an evaluation: their bid, their scores, and their rank.
 *
 * <p>Scores are computed by the service, never supplied by the caller — a caller can set
 * raw criterion scores, but not the derived totals.
 */
@Entity
@Table(name = "participant_evaluation",
    uniqueConstraints = @UniqueConstraint(columnNames = {"evaluation_id", "supplier_code"}))
public class ParticipantEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    /** Supplier code owned by the Vendor Management service. */
    @Column(name = "supplier_code", nullable = false, length = 50)
    private String supplierCode;

    /** The bid amount carried over from the opened tender. */
    @Column(name = "bid_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal bidAmount;

    @Column(name = "technical_score", precision = 6, scale = 2)
    private BigDecimal technicalScore;

    @Column(name = "financial_score", precision = 6, scale = 2)
    private BigDecimal financialScore;

    @Column(name = "combined_score", precision = 6, scale = 2)
    private BigDecimal combinedScore;

    /** Null until the technical stage is closed. */
    @Column(name = "qualified")
    private Boolean qualified;

    /** Rank among qualified participants; null if disqualified. */
    @Column(name = "final_rank")
    private Integer rank;

    protected ParticipantEvaluation() {
    }

    public ParticipantEvaluation(Evaluation evaluation, String supplierCode, BigDecimal bidAmount) {
        if (bidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bid amount must be greater than zero");
        }
        this.evaluation = evaluation;
        this.supplierCode = supplierCode;
        this.bidAmount = bidAmount;
    }

    public void applyTechnicalResult(BigDecimal technicalScore, boolean qualified) {
        this.technicalScore = technicalScore;
        this.qualified = qualified;
    }

    public void applyFinancialResult(BigDecimal financialScore, BigDecimal combinedScore) {
        this.financialScore = financialScore;
        this.combinedScore = combinedScore;
    }

    public void assignRank(Integer rank) {
        this.rank = rank;
    }

    public Long getId() { return id; }
    public Evaluation getEvaluation() { return evaluation; }
    public String getSupplierCode() { return supplierCode; }
    public BigDecimal getBidAmount() { return bidAmount; }
    public BigDecimal getTechnicalScore() { return technicalScore; }
    public BigDecimal getFinancialScore() { return financialScore; }
    public BigDecimal getCombinedScore() { return combinedScore; }
    public Boolean getQualified() { return qualified; }
    public Integer getRank() { return rank; }
}
