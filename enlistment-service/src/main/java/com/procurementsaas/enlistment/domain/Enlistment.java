package com.procurementsaas.enlistment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * The outcome: this supplier is qualified for this category, until this date.
 *
 * <p>Deliberately a record with an expiry rather than a flag on the supplier. Validity is
 * derived from dates on every check, so an enlistment lapses on its own without a nightly
 * job having to remember to switch it off.
 */
@Entity
@Table(name = "enlistment")
public class Enlistment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_code", nullable = false, length = 50)
    private String supplierCode;

    /** Master Data category the supplier is qualified for. */
    @Column(name = "category_code", nullable = false, length = 50)
    private String categoryCode;

    @Column(name = "schedule_code", nullable = false, length = 50)
    private String scheduleCode;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnlistmentStatus status = EnlistmentStatus.ACTIVE;

    @Column(name = "revoked_reason", length = 500)
    private String revokedReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Enlistment() {
    }

    public Enlistment(String supplierCode, String categoryCode, String scheduleCode,
                      LocalDate validFrom, int validityMonths) {
        this.supplierCode = supplierCode;
        this.categoryCode = categoryCode;
        this.scheduleCode = scheduleCode;
        this.validFrom = validFrom;
        this.validUntil = validFrom.plusMonths(validityMonths);
    }

    /**
     * Whether the supplier may bid in this category today.
     *
     * <p>Expiry is computed, not stored: a lapsed enlistment stops counting the day it
     * runs out, whether or not anything has run to notice.
     */
    public boolean isCurrentlyValid() {
        return status == EnlistmentStatus.ACTIVE
            && !LocalDate.now().isBefore(validFrom)
            && !LocalDate.now().isAfter(validUntil);
    }

    public boolean hasExpired() {
        return LocalDate.now().isAfter(validUntil);
    }

    /**
     * Withdraws the enlistment early — typically because the supplier was debarred.
     * Revoked, not deleted: why someone lost their standing must remain answerable.
     */
    public void revoke(String reason) {
        if (status == EnlistmentStatus.REVOKED) {
            return;   // already withdrawn; revoking again is a no-op, not an error
        }
        this.status = EnlistmentStatus.REVOKED;
        this.revokedReason = reason;
    }

    public String effectiveStatus() {
        if (status == EnlistmentStatus.REVOKED) {
            return EnlistmentStatus.REVOKED.name();
        }
        return hasExpired() ? EnlistmentStatus.EXPIRED.name() : EnlistmentStatus.ACTIVE.name();
    }

    public Long getId() { return id; }
    public String getSupplierCode() { return supplierCode; }
    public String getCategoryCode() { return categoryCode; }
    public String getScheduleCode() { return scheduleCode; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidUntil() { return validUntil; }
    public EnlistmentStatus getStatus() { return status; }
    public String getRevokedReason() { return revokedReason; }
    public Instant getCreatedAt() { return createdAt; }
}
