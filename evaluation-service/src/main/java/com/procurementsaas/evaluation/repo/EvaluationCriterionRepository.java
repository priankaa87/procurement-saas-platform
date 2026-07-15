package com.procurementsaas.evaluation.repo;

import com.procurementsaas.evaluation.domain.EvaluationCriterion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationCriterionRepository extends JpaRepository<EvaluationCriterion, Long> {
    List<EvaluationCriterion> findByEvaluationId(Long evaluationId);
    Optional<EvaluationCriterion> findByEvaluationIdAndCode(Long evaluationId, String code);
    boolean existsByEvaluationIdAndCode(Long evaluationId, String code);
}
