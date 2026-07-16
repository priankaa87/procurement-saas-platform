package com.procurementsaas.workflow.domain;

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

/** One rung of a workflow: who must agree, and in what order. */
@Entity
@Table(name = "approval_step",
    uniqueConstraints = @UniqueConstraint(columnNames = {"workflow_id", "step_no"}))
public class ApprovalStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private ApprovalWorkflow workflow;

    @Column(name = "step_no", nullable = false)
    private int stepNo;

    @Column(nullable = false, length = 150)
    private String name;

    /** The role a person must hold — directly or by delegation — to act on this step. */
    @Column(name = "role_code", nullable = false, length = 60)
    private String roleCode;

    protected ApprovalStep() {
    }

    public ApprovalStep(ApprovalWorkflow workflow, int stepNo, String name, String roleCode) {
        if (stepNo < 1) {
            throw new IllegalArgumentException("Step number must be 1 or greater");
        }
        this.workflow = workflow;
        this.stepNo = stepNo;
        this.name = name;
        this.roleCode = roleCode;
    }

    public Long getId() { return id; }
    public ApprovalWorkflow getWorkflow() { return workflow; }
    public int getStepNo() { return stepNo; }
    public String getName() { return name; }
    public String getRoleCode() { return roleCode; }
}
