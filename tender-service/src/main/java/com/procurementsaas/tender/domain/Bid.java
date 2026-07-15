package com.procurementsaas.tender.domain;

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
import java.time.Instant;

/**
 * A sealed bid from a supplier.
 *
 * <p>Rows exist from the moment of submission, but the service refuses to disclose them
 * until the tender has been opened — the database is the vault, the service is the lock.
 * One bid per supplier per tender, enforced by a unique constraint.
 */
@Entity
@Table(name = "bid",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tender_id", "supplier_code"}))
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tender_id", nullable = false)
    private Tender tender;

    @Column(name = "supplier_code", nullable = false, length = 50)
    private String supplierCode;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(length = 1000)
    private String notes;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt = Instant.now();

    protected Bid() {
    }

    public Bid(Tender tender, String supplierCode, BigDecimal totalAmount, String currencyCode,
               String notes) {
        this.tender = tender;
        this.supplierCode = supplierCode;
        this.totalAmount = totalAmount;
        this.currencyCode = currencyCode;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public Tender getTender() { return tender; }
    public String getSupplierCode() { return supplierCode; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrencyCode() { return currencyCode; }
    public String getNotes() { return notes; }
    public Instant getSubmittedAt() { return submittedAt; }
}
