package com.procurementsaas.workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A named approval route, e.g. "Tender publication needs procurement then finance".
 *
 * <p>{@code subjectType} is a free string like {@code TENDER} rather than an enum: this
 * engine should not need a code change to start approving something new.
 */
@Entity
@Table(name = "approval_workflow")
public class ApprovalWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "subject_type", nullable = false, length = 40)
    private String subjectType;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ApprovalWorkflow() {
    }

    public ApprovalWorkflow(String code, String name, String subjectType) {
        this.code = code;
        this.name = name;
        this.subjectType = subjectType;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSubjectType() { return subjectType; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
