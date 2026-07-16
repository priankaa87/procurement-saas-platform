package com.procurementsaas.contract.domain;

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
import java.time.LocalDate;

/**
 * A notice of award: the offer made to the winning supplier.
 *
 * <p>The amount is captured here rather than looked up, because it is what was offered at
 * the moment of award and must not drift afterwards.
 */
@Entity
@Table(name = "award")
public class Award {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tender this award came from; one award per tender. */
    @Column(name = "tender_code", nullable = false, unique = true, length = 50)
    private String tenderCode;

    @Column(name = "tender_title", length = 250)
    private String tenderTitle;

    @Column(name = "supplier_code", nullable = false, length = 50)
    private String supplierCode;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private AwardStatus status = AwardStatus.PENDING_ACCEPTANCE;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt = Instant.now();

    /** Last day the supplier may accept. */
    @Column(name = "respond_by", nullable = false)
    private LocalDate respondBy;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "decline_reason", length = 500)
    private String declineReason;

    protected Award() {
    }

    public Award(String tenderCode, String tenderTitle, String supplierCode, BigDecimal amount,
                 String currencyCode, LocalDate respondBy) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Award amount must be greater than zero");
        }
        this.tenderCode = tenderCode;
        this.tenderTitle = tenderTitle;
        this.supplierCode = supplierCode;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.respondBy = respondBy;
    }

    /** True once the acceptance window has closed. */
    public boolean hasLapsed() {
        return LocalDate.now().isAfter(respondBy);
    }

    /**
     * The supplier accepts the award.
     *
     * <p>An offer that has run out of time cannot be accepted — otherwise a supplier could
     * sit on an award, watch the market move, and accept months later on the old terms.
     */
    public void accept() {
        requirePending();
        if (hasLapsed()) {
            throw new IllegalStateException(
                "The acceptance window for " + tenderCode + " closed on " + respondBy);
        }
        this.status = AwardStatus.ACCEPTED;
        this.respondedAt = Instant.now();
    }

    public void decline(String reason) {
        requirePending();
        this.status = AwardStatus.DECLINED;
        this.declineReason = reason;
        this.respondedAt = Instant.now();
    }

    /** Marks an unanswered award as lapsed. */
    public void expire() {
        requirePending();
        if (!hasLapsed()) {
            throw new IllegalStateException(
                "Award " + tenderCode + " has not lapsed yet; it is open until " + respondBy);
        }
        this.status = AwardStatus.EXPIRED;
    }

    public void cancel() {
        if (status == AwardStatus.ACCEPTED) {
            throw new IllegalStateException(
                "An accepted award cannot be cancelled here; cancel the work order: " + tenderCode);
        }
        this.status = AwardStatus.CANCELLED;
    }

    public boolean isAccepted() {
        return status == AwardStatus.ACCEPTED;
    }

    private void requirePending() {
        if (status != AwardStatus.PENDING_ACCEPTANCE) {
            throw new IllegalStateException(
                "Award " + tenderCode + " has already been answered (status " + status + ")");
        }
    }

    public Long getId() { return id; }
    public String getTenderCode() { return tenderCode; }
    public String getTenderTitle() { return tenderTitle; }
    public String getSupplierCode() { return supplierCode; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrencyCode() { return currencyCode; }
    public AwardStatus getStatus() { return status; }
    public Instant getIssuedAt() { return issuedAt; }
    public LocalDate getRespondBy() { return respondBy; }
    public Instant getRespondedAt() { return respondedAt; }
    public String getDeclineReason() { return declineReason; }
}
