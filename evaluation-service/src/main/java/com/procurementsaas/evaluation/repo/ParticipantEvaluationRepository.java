package com.procurementsaas.evaluation.repo;

import com.procurementsaas.evaluation.domain.ParticipantEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipantEvaluationRepository extends JpaRepository<ParticipantEvaluation, Long> {
    List<ParticipantEvaluation> findByEvaluationId(Long evaluationId);
    Optional<ParticipantEvaluation> findByEvaluationIdAndSupplierCode(Long evaluationId, String supplierCode);
    boolean existsByEvaluationIdAndSupplierCode(Long evaluationId, String supplierCode);
}
