package com.procurementsaas.evaluation.domain;

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

/** A raw 0–100 score awarded to one participant against one technical criterion. */
@Entity
@Table(name = "criterion_score",
    uniqueConstraints = @UniqueConstraint(columnNames = {"participant_evaluation_id", "criterion_id"}))
public class CriterionScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_evaluation_id", nullable = false)
    private ParticipantEvaluation participantEvaluation;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "criterion_id", nullable = false)
    private EvaluationCriterion criterion;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal score;

    @Column(length = 500)
    private String comment;

    protected CriterionScore() {
    }

    public CriterionScore(ParticipantEvaluation participantEvaluation,
                          EvaluationCriterion criterion, BigDecimal score, String comment) {
        validate(score);
        this.participantEvaluation = participantEvaluation;
        this.criterion = criterion;
        this.score = score;
        this.comment = comment;
    }

    public void update(BigDecimal score, String comment) {
        validate(score);
        this.score = score;
        this.comment = comment;
    }

    private static void validate(BigDecimal score) {
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Score must be between 0 and 100, got " + score);
        }
    }

    public Long getId() { return id; }
    public ParticipantEvaluation getParticipantEvaluation() { return participantEvaluation; }
    public EvaluationCriterion getCriterion() { return criterion; }
    public BigDecimal getScore() { return score; }
    public String getComment() { return comment; }
}
