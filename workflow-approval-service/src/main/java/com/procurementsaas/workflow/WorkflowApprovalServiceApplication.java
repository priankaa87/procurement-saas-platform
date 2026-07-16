package com.procurementsaas.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Workflow &amp; Approval Service — the platform's approval engine.
 *
 * <p>Tenders, awards, debarments and enlistments all need "somebody senior must agree to
 * this before it happens". Rather than each service growing its own half of that idea,
 * they raise an approval request here against a configured workflow and act on the outcome.
 *
 * <p>Two controls are the reason this service exists at all:
 *
 * <ul>
 *   <li><strong>Separation of duties</strong> — nobody approves their own request.</li>
 *   <li><strong>Delegation of authority</strong> — when an approver is away, a delegate may
 *       act in their place for a bounded period, and the record shows both who acted and
 *       whose authority they used.</li>
 * </ul>
 *
 * <p>The subject of an approval is held as a type and a business reference
 * ({@code TENDER}/{@code T-001}) rather than a foreign key: this engine must not need to
 * know what a tender is.
 */
@SpringBootApplication
public class WorkflowApprovalServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkflowApprovalServiceApplication.class, args);
    }
}
