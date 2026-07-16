package com.procurementsaas.enlistment.repo;

import com.procurementsaas.enlistment.domain.CriterionAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CriterionAssessmentRepository extends JpaRepository<CriterionAssessment, Long> {
    List<CriterionAssessment> findByApplicationId(Long applicationId);
    Optional<CriterionAssessment> findByApplicationIdAndCriterionId(Long applicationId, Long criterionId);
}
