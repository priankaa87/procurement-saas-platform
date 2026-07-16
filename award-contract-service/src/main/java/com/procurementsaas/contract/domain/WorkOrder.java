package com.procurementsaas.contract.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/** The instruction to supply, raised from an accepted award. */
@Entity
@Table(name = "work_order")
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "award_id", nullable = false)
    private Award award;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkOrderStatus status = WorkOrderStatus.DRAFT;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected WorkOrder() {
    }

    public WorkOrder(Award award, String code) {
        if (!award.isAccepted()) {
            throw new IllegalStateException(
                "A work order can only be raised from an accepted award; " + award.getTenderCode()
                    + " is " + award.getStatus());
        }
        this.award = award;
        this.code = code;
        this.totalAmount = award.getAmount();
        this.currencyCode = award.getCurrencyCode();
    }

    public boolean isEditable() {
        return status == WorkOrderStatus.DRAFT;
    }

    /**
     * Issues the order to the supplier.
     *
     * @param lineCount number of delivery lines; an order with nothing to deliver is not an order
     */
    public void issue(long lineCount) {
        if (status != WorkOrderStatus.DRAFT) {
            throw new IllegalStateException("Work order " + code + " is already issued");
        }
        if (lineCount == 0) {
            throw new IllegalStateException(
                "Cannot issue work order " + code + " with no delivery lines");
        }
        this.status = WorkOrderStatus.ISSUED;
        this.issuedAt = Instant.now();
    }

    /** Called on the first receipt against this order. */
    public void markInProgress() {
        if (status == WorkOrderStatus.ISSUED) {
            this.status = WorkOrderStatus.IN_PROGRESS;
        }
    }

    /** Called once every line is fully delivered. */
    public void complete() {
        if (status != WorkOrderStatus.ISSUED && status != WorkOrderStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                "Work order " + code + " cannot be completed from " + status);
        }
        this.status = WorkOrderStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void cancel() {
        if (status == WorkOrderStatus.COMPLETED) {
            throw new IllegalStateException("A completed work order cannot be cancelled: " + code);
        }
        this.status = WorkOrderStatus.CANCELLED;
    }

    public boolean acceptsReceipts() {
        return status == WorkOrderStatus.ISSUED || status == WorkOrderStatus.IN_PROGRESS;
    }

    public Long getId() { return id; }
    public Award getAward() { return award; }
    public String getCode() { return code; }
    public WorkOrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrencyCode() { return currencyCode; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
