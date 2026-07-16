package com.procurementsaas.workflow.repo;

import com.procurementsaas.workflow.domain.ApprovalAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalActionRepository extends JpaRepository<ApprovalAction, Long> {
    List<ApprovalAction> findByRequestIdOrderByActedAt(Long requestId);
}
