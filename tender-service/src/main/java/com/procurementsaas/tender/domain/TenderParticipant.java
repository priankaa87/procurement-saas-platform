package com.procurementsaas.tender.domain;

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

import java.time.Instant;

/**
 * A supplier invited to a tender. {@code supplierCode} is owned by the Vendor Management
 * service; only invited suppliers may bid.
 */
@Entity
@Table(name = "tender_participant",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tender_id", "supplier_code"}))
public class TenderParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tender_id", nullable = false)
    private Tender tender;

    @Column(name = "supplier_code", nullable = false, length = 50)
    private String supplierCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipantStatus status = ParticipantStatus.INVITED;

    @Column(name = "invited_at", nullable = false)
    private Instant invitedAt = Instant.now();

    protected TenderParticipant() {
    }

    public TenderParticipant(Tender tender, String supplierCode) {
        this.tender = tender;
        this.supplierCode = supplierCode;
    }

    public void markBidSubmitted() {
        this.status = ParticipantStatus.BID_SUBMITTED;
    }

    public void withdraw() {
        this.status = ParticipantStatus.WITHDRAWN;
    }

    public Long getId() { return id; }
    public Tender getTender() { return tender; }
    public String getSupplierCode() { return supplierCode; }
    public ParticipantStatus getStatus() { return status; }
    public Instant getInvitedAt() { return invitedAt; }
}
