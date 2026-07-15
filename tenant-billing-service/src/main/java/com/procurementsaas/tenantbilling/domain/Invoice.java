package com.procurementsaas.tenantbilling.domain;

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
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A charge for one billing period.
 *
 * <p>The plan code, price, and currency are copied onto the invoice rather than read
 * through a relation: an invoice must always show what was actually charged at the time,
 * even after the plan's price changes or the plan is withdrawn.
 */
@Entity
@Table(name = "invoice",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "period_start"}))
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "plan_code", nullable = false, length = 40)
    private String planCode;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.ISSUED;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt = Instant.now();

    @Column(name = "paid_at")
    private Instant paidAt;

    protected Invoice() {
    }

    public Invoice(Tenant tenant, String planCode, LocalDate periodStart, LocalDate periodEnd,
                   BigDecimal amount, String currencyCode) {
        this.tenant = tenant;
        this.planCode = planCode;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.amount = amount;
        this.currencyCode = currencyCode;
    }

    public void markPaid() {
        if (status == InvoiceStatus.VOID) {
            throw new IllegalStateException("A void invoice cannot be paid: " + id);
        }
        this.status = InvoiceStatus.PAID;
        this.paidAt = Instant.now();
    }

    public void voidInvoice() {
        if (status == InvoiceStatus.PAID) {
            throw new IllegalStateException("A paid invoice cannot be voided: " + id);
        }
        this.status = InvoiceStatus.VOID;
    }

    public Long getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public String getPlanCode() { return planCode; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrencyCode() { return currencyCode; }
    public InvoiceStatus getStatus() { return status; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getPaidAt() { return paidAt; }
}
