package com.procurementsaas.enlistment.domain;

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

/** One supplier's bid to be pre-qualified in a round. */
@Entity
@Table(name = "enlistment_application",
    uniqueConstraints = @UniqueConstraint(columnNames = {"schedule_id", "supplier_code"}))
public class EnlistmentApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private EnlistmentSchedule schedule;

    @Column(name = "supplier_code", nullable = false, length = 50)
    private String supplierCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    @Column(precision = 6, scale = 2)
    private BigDecimal score;

    /** Set on rejection, so a supplier can be told why rather than merely that. */
    @Column(name = "decision_reason", length = 500)
    private String decisionReason;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt = Instant.now();

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected EnlistmentApplication() {
    }

    public EnlistmentApplication(EnlistmentSchedule schedule, String supplierCode) {
        this.schedule = schedule;
        this.supplierCode = supplierCode;
    }

    public boolean isOpen() {
        return status == ApplicationStatus.SUBMITTED;
    }

    public void qualify(BigDecimal score) {
        requireOpen();
        this.status = ApplicationStatus.QUALIFIED;
        this.score = score;
        this.decidedAt = Instant.now();
    }

    public void reject(BigDecimal score, String reason) {
        requireOpen();
        this.status = ApplicationStatus.REJECTED;
        this.score = score;
        this.decisionReason = reason;
        this.decidedAt = Instant.now();
    }

    /** An applicant may pull out while the round is still open. */
    public void withdraw() {
        requireOpen();
        this.status = ApplicationStatus.WITHDRAWN;
        this.decidedAt = Instant.now();
    }

    private void requireOpen() {
        if (status != ApplicationStatus.SUBMITTED) {
            throw new IllegalStateException(
                "Application by " + supplierCode + " is already " + status);
        }
    }

    public Long getId() { return id; }
    public EnlistmentSchedule getSchedule() { return schedule; }
    public String getSupplierCode() { return supplierCode; }
    public ApplicationStatus getStatus() { return status; }
    public BigDecimal getScore() { return score; }
    public String getDecisionReason() { return decisionReason; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getDecidedAt() { return decidedAt; }
}
