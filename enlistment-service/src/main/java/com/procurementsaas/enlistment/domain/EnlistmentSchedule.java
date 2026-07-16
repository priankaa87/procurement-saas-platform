package com.procurementsaas.enlistment.domain;

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
 * A pre-qualification round for one category of work.
 *
 * <p>{@code categoryCode} belongs to Master Data. {@code validityMonths} is how long a
 * successful applicant stays qualified — enlistment expires because the evidence behind it
 * does.
 */
@Entity
@Table(name = "enlistment_schedule")
public class EnlistmentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "category_code", nullable = false, length = 50)
    private String categoryCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleStatus status = ScheduleStatus.DRAFT;

    @Column(name = "application_deadline", nullable = false)
    private Instant applicationDeadline;

    /** Minimum weighted score to qualify, 0–100. */
    @Column(name = "pass_mark", nullable = false, precision = 5, scale = 2)
    private BigDecimal passMark;

    @Column(name = "validity_months", nullable = false)
    private int validityMonths;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "published_at")
    private Instant publishedAt;

    protected EnlistmentSchedule() {
    }

    public EnlistmentSchedule(String code, String title, String description, String categoryCode,
                              Instant applicationDeadline, BigDecimal passMark, int validityMonths) {
        if (passMark.compareTo(BigDecimal.ZERO) < 0
            || passMark.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Pass mark must be between 0 and 100");
        }
        if (validityMonths < 1) {
            throw new IllegalArgumentException("Validity must be at least one month");
        }
        this.code = code;
        this.title = title;
        this.description = description;
        this.categoryCode = categoryCode;
        this.applicationDeadline = applicationDeadline;
        this.passMark = passMark;
        this.validityMonths = validityMonths;
    }

    public boolean isEditable() {
        return status == ScheduleStatus.DRAFT;
    }

    /**
     * Opens the round to applicants.
     *
     * @param criteriaCount criteria must exist first — you cannot ask suppliers to meet a
     *                      standard you have not written down
     */
    public void publish(long criteriaCount) {
        if (status != ScheduleStatus.DRAFT) {
            throw new IllegalStateException("Only a DRAFT schedule can be published: " + code);
        }
        if (criteriaCount == 0) {
            throw new IllegalStateException(
                "Cannot publish " + code + " with no criteria: applicants must know the standard");
        }
        if (applicationDeadline.isBefore(Instant.now())) {
            throw new IllegalStateException(
                "Cannot publish " + code + " with a deadline in the past");
        }
        this.status = ScheduleStatus.OPEN;
        this.publishedAt = Instant.now();
    }

    public boolean acceptingApplications() {
        return status == ScheduleStatus.OPEN && Instant.now().isBefore(applicationDeadline);
    }

    /** Closes the round so assessment can begin. */
    public void close() {
        if (status != ScheduleStatus.OPEN) {
            throw new IllegalStateException("Only an OPEN schedule can be closed: " + code);
        }
        if (Instant.now().isBefore(applicationDeadline)) {
            throw new IllegalStateException(
                "Cannot close " + code + " before its deadline: applicants still have time");
        }
        this.status = ScheduleStatus.CLOSED;
    }

    public void complete() {
        if (status != ScheduleStatus.CLOSED) {
            throw new IllegalStateException("Only a CLOSED schedule can be completed: " + code);
        }
        this.status = ScheduleStatus.COMPLETED;
    }

    public boolean underAssessment() {
        return status == ScheduleStatus.CLOSED;
    }

    public void cancel() {
        if (status == ScheduleStatus.COMPLETED) {
            throw new IllegalStateException("A completed schedule cannot be cancelled: " + code);
        }
        this.status = ScheduleStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategoryCode() { return categoryCode; }
    public ScheduleStatus getStatus() { return status; }
    public Instant getApplicationDeadline() { return applicationDeadline; }
    public BigDecimal getPassMark() { return passMark; }
    public int getValidityMonths() { return validityMonths; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
}
