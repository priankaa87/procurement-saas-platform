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

/** A weighted technical criterion, e.g. "Delivery capability", weight 30. */
@Entity
@Table(name = "evaluation_criterion",
    uniqueConstraints = @UniqueConstraint(columnNames = {"evaluation_id", "code"}))
public class EvaluationCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    /** Relative weight; all criteria in an evaluation must total 100. */
    @Column(nullable = false)
    private int weight;

    protected EvaluationCriterion() {
    }

    public EvaluationCriterion(Evaluation evaluation, String code, String name, int weight) {
        if (weight <= 0 || weight > 100) {
            throw new IllegalArgumentException("Criterion weight must be between 1 and 100");
        }
        this.evaluation = evaluation;
        this.code = code;
        this.name = name;
        this.weight = weight;
    }

    public Long getId() { return id; }
    public Evaluation getEvaluation() { return evaluation; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getWeight() { return weight; }
}
