package com.procurementsaas.workflow.repo;

import com.procurementsaas.workflow.domain.ApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {
    List<ApprovalStep> findByWorkflowIdOrderByStepNo(Long workflowId);
    Optional<ApprovalStep> findByWorkflowIdAndStepNo(Long workflowId, int stepNo);
    long countByWorkflowId(Long workflowId);
}
