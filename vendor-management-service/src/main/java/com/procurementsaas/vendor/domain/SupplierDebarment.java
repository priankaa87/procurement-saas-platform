package com.procurementsaas.vendor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A debarment decision that blocks a supplier from participating. Kept as an immutable
 * audit record — reinstatement closes the record rather than deleting it.
 */
@Entity
@Table(name = "supplier_debarment")
public class SupplierDebarment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "debarred_from", nullable = false)
    private LocalDate debarredFrom;

    /** Null means indefinite. */
    @Column(name = "debarred_until")
    private LocalDate debarredUntil;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected SupplierDebarment() {
    }

    public SupplierDebarment(Supplier supplier, String reason, LocalDate debarredFrom,
                             LocalDate debarredUntil) {
        this.supplier = supplier;
        this.reason = reason;
        this.debarredFrom = debarredFrom;
        this.debarredUntil = debarredUntil;
    }

    public void close() {
        this.active = false;
    }

    public Long getId() { return id; }
    public Supplier getSupplier() { return supplier; }
    public String getReason() { return reason; }
    public LocalDate getDebarredFrom() { return debarredFrom; }
    public LocalDate getDebarredUntil() { return debarredUntil; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
