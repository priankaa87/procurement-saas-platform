package com.procurementsaas.evaluation.repo;

import com.procurementsaas.evaluation.domain.CriterionScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CriterionScoreRepository extends JpaRepository<CriterionScore, Long> {
    List<CriterionScore> findByParticipantEvaluationId(Long participantEvaluationId);
    Optional<CriterionScore> findByParticipantEvaluationIdAndCriterionId(
        Long participantEvaluationId, Long criterionId);
}
