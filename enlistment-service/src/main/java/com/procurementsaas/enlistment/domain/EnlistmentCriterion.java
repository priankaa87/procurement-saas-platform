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

/**
 * A published standard applicants are judged against.
 *
 * <p>{@code mandatory} is not just a heavier weight. A mandatory criterion is a gate: fail
 * it and you are out regardless of how well you scored elsewhere. "Holds a valid trade
 * licence" cannot be compensated for by being cheap.
 */
@Entity
@Table(name = "enlistment_criterion",
    uniqueConstraints = @UniqueConstraint(columnNames = {"schedule_id", "code"}))
public class EnlistmentCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private EnlistmentSchedule schedule;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    /** Weight toward the score; all criteria in a schedule must total 100. */
    @Column(nullable = false)
    private int weight;

    /** A gate, not a weight: failing this disqualifies outright. */
    @Column(nullable = false)
    private boolean mandatory = false;

    protected EnlistmentCriterion() {
    }

    public EnlistmentCriterion(EnlistmentSchedule schedule, String code, String name, int weight,
                               boolean mandatory) {
        if (weight <= 0 || weight > 100) {
            throw new IllegalArgumentException("Criterion weight must be between 1 and 100");
        }
        this.schedule = schedule;
        this.code = code;
        this.name = name;
        this.weight = weight;
        this.mandatory = mandatory;
    }

    public Long getId() { return id; }
    public EnlistmentSchedule getSchedule() { return schedule; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getWeight() { return weight; }
    public boolean isMandatory() { return mandatory; }
}
