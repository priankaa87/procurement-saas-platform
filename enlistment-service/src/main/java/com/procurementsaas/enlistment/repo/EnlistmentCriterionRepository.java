package com.procurementsaas.enlistment.repo;

import com.procurementsaas.enlistment.domain.EnlistmentCriterion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnlistmentCriterionRepository extends JpaRepository<EnlistmentCriterion, Long> {
    List<EnlistmentCriterion> findByScheduleId(Long scheduleId);
    Optional<EnlistmentCriterion> findByScheduleIdAndCode(Long scheduleId, String code);
    boolean existsByScheduleIdAndCode(Long scheduleId, String code);
    long countByScheduleId(Long scheduleId);
}
