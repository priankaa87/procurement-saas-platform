package com.procurementsaas.workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A request for something to be approved, walking one step at a time through its workflow.
 *
 * <p>The subject is a type and a business reference — this engine never learns what a
 * tender is.
 */
@Entity
@Table(name = "approval_request")
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_code", nullable = false, length = 50)
    private String workflowCode;

    @Column(name = "subject_type", nullable = false, length = 40)
    private String subjectType;

    /** e.g. the tender code the approval is about. */
    @Column(name = "subject_ref", nullable = false, length = 80)
    private String subjectRef;

    @Column(name = "requested_by", nullable = false, length = 100)
    private String requestedBy;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    /** The step awaiting a decision; frozen once the request finishes. */
    @Column(name = "current_step", nullable = false)
    private int currentStep = 1;

    @Column(name = "total_steps", nullable = false)
    private int totalSteps;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ApprovalRequest() {
    }

    public ApprovalRequest(String workflowCode, String subjectType, String subjectRef,
                           String requestedBy, String reason, int totalSteps) {
        if (totalSteps < 1) {
            throw new IllegalStateException(
                "Workflow " + workflowCode + " has no steps, so nothing can be approved");
        }
        this.workflowCode = workflowCode;
        this.subjectType = subjectType;
        this.subjectRef = subjectRef;
        this.requestedBy = requestedBy;
        this.reason = reason;
        this.totalSteps = totalSteps;
    }

    public boolean isOpen() {
        return status == ApprovalStatus.PENDING;
    }

    /**
     * Records approval of the current step, advancing to the next or finishing the request.
     *
     * @return true if this approval completed the whole request
     */
    public boolean approveCurrentStep() {
        requireOpen();
        if (currentStep >= totalSteps) {
            this.status = ApprovalStatus.APPROVED;
            this.completedAt = Instant.now();
            return true;
        }
        this.currentStep++;
        return false;
    }

    /** One rejection ends it: an approval chain is a series of vetoes, not a vote. */
    public void reject() {
        requireOpen();
        this.status = ApprovalStatus.REJECTED;
        this.completedAt = Instant.now();
    }

    public void cancel() {
        requireOpen();
        this.status = ApprovalStatus.CANCELLED;
        this.completedAt = Instant.now();
    }

    /**
     * The control that stops a request being nodded through by the person who raised it.
     * Deliberately not overridable: if someone genuinely must approve their own work, the
     * workflow is wrong, not this check.
     */
    public boolean wasRaisedBy(String actorId) {
        return requestedBy.equals(actorId);
    }

    private void requireOpen() {
        if (status != ApprovalStatus.PENDING) {
            throw new IllegalStateException(
                "Approval request " + id + " is already " + status);
        }
    }

    public Long getId() { return id; }
    public String getWorkflowCode() { return workflowCode; }
    public String getSubjectType() { return subjectType; }
    public String getSubjectRef() { return subjectRef; }
    public String getRequestedBy() { return requestedBy; }
    public String getReason() { return reason; }
    public ApprovalStatus getStatus() { return status; }
    public int getCurrentStep() { return currentStep; }
    public int getTotalSteps() { return totalSteps; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
}
