package com.procurementsaas.workflow.repo;

import com.procurementsaas.workflow.domain.ApprovalWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, Long> {
    Optional<ApprovalWorkflow> findByCode(String code);
    boolean existsByCode(String code);
    List<ApprovalWorkflow> findBySubjectType(String subjectType);
}
