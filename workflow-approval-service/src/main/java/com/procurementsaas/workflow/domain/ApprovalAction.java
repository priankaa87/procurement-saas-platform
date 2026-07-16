package com.procurementsaas.workflow.domain;

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

import java.time.Instant;

/**
 * A decision taken on one step. Append-only: this is the audit trail that answers "who
 * agreed to this, and on what authority?" long after the fact.
 *
 * <p>{@code actorId} is always the human who actually clicked. {@code onBehalfOf} is set
 * only when they were acting under a delegation, and names the person whose authority was
 * used. Collapsing the two into one field would quietly erase the difference between an
 * approver and their stand-in.
 */
@Entity
@Table(name = "approval_action")
public class ApprovalAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private ApprovalRequest request;

    @Column(name = "step_no", nullable = false)
    private int stepNo;

    @Column(name = "role_code", nullable = false, length = 60)
    private String roleCode;

    /** Who actually acted. */
    @Column(name = "actor_id", nullable = false, length = 100)
    private String actorId;

    /** Whose authority was used, when acting under a delegation. Null otherwise. */
    @Column(name = "on_behalf_of", length = 100)
    private String onBehalfOf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalDecision decision;

    @Column(length = 500)
    private String comment;

    @Column(name = "acted_at", nullable = false)
    private Instant actedAt = Instant.now();

    protected ApprovalAction() {
    }

    public ApprovalAction(ApprovalRequest request, int stepNo, String roleCode, String actorId,
                          String onBehalfOf, ApprovalDecision decision, String comment) {
        this.request = request;
        this.stepNo = stepNo;
        this.roleCode = roleCode;
        this.actorId = actorId;
        this.onBehalfOf = onBehalfOf;
        this.decision = decision;
        this.comment = comment;
    }

    public boolean wasDelegated() {
        return onBehalfOf != null;
    }

    public Long getId() { return id; }
    public ApprovalRequest getRequest() { return request; }
    public int getStepNo() { return stepNo; }
    public String getRoleCode() { return roleCode; }
    public String getActorId() { return actorId; }
    public String getOnBehalfOf() { return onBehalfOf; }
    public ApprovalDecision getDecision() { return decision; }
    public String getComment() { return comment; }
    public Instant getActedAt() { return actedAt; }
}
