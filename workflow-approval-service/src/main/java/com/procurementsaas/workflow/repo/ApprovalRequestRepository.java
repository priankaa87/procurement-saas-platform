package com.procurementsaas.workflow.repo;

import com.procurementsaas.workflow.domain.ApprovalRequest;
import com.procurementsaas.workflow.domain.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    List<ApprovalRequest> findByStatusOrderByCreatedAt(ApprovalStatus status);
    List<ApprovalRequest> findBySubjectTypeAndSubjectRefOrderByCreatedAtDesc(
        String subjectType, String subjectRef);

    /** An open request for the same subject — you cannot ask twice while one is live. */
    Optional<ApprovalRequest> findBySubjectTypeAndSubjectRefAndStatus(
        String subjectType, String subjectRef, ApprovalStatus status);
}
