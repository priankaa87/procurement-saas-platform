package com.procurementsaas.enlistment.domain;

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
 * The committee's judgement of one applicant against one criterion.
 *
 * <p>{@code met} is recorded separately from {@code score} because they answer different
 * questions. A mandatory criterion is pass/fail — 60 out of 100 on "holds a valid licence"
 * is meaningless; either they hold one or they do not.
 */
@Entity
@Table(name = "criterion_assessment",
    uniqueConstraints = @UniqueConstraint(columnNames = {"application_id", "criterion_id"}))
public class CriterionAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private EnlistmentApplication application;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "criterion_id", nullable = false)
    private EnlistmentCriterion criterion;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal score;

    /** Whether the criterion was satisfied at all. Decisive when the criterion is mandatory. */
    @Column(nullable = false)
    private boolean met;

    @Column(length = 500)
    private String comment;

    protected CriterionAssessment() {
    }

    public CriterionAssessment(EnlistmentApplication application, EnlistmentCriterion criterion,
                               BigDecimal score, boolean met, String comment) {
        validate(score);
        this.application = application;
        this.criterion = criterion;
        this.score = score;
        this.met = met;
        this.comment = comment;
    }

    public void update(BigDecimal score, boolean met, String comment) {
        validate(score);
        this.score = score;
        this.met = met;
        this.comment = comment;
    }

    private static void validate(BigDecimal score) {
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Score must be between 0 and 100, got " + score);
        }
    }

    public Long getId() { return id; }
    public EnlistmentApplication getApplication() { return application; }
    public EnlistmentCriterion getCriterion() { return criterion; }
    public BigDecimal getScore() { return score; }
    public boolean isMet() { return met; }
    public String getComment() { return comment; }
}
