package com.procurementsaas.enlistment.service;

import com.procurementsaas.enlistment.domain.CriterionAssessment;
import com.procurementsaas.enlistment.domain.Enlistment;
import com.procurementsaas.enlistment.domain.EnlistmentApplication;
import com.procurementsaas.enlistment.domain.EnlistmentCriterion;
import com.procurementsaas.enlistment.domain.EnlistmentSchedule;
import com.procurementsaas.enlistment.dto.Dtos.ApplicationDto;
import com.procurementsaas.enlistment.dto.Dtos.AssessmentDto;
import com.procurementsaas.enlistment.dto.Dtos.CriterionDto;
import com.procurementsaas.enlistment.dto.Dtos.EnlistmentDto;
import com.procurementsaas.enlistment.dto.Dtos.ScheduleDto;

/** Maps enlistment entities to API DTOs. */
public final class EnlistmentMapper {

    private EnlistmentMapper() {
    }

    public static ScheduleDto toDto(EnlistmentSchedule s, long criteriaCount) {
        return new ScheduleDto(s.getId(), s.getCode(), s.getTitle(), s.getDescription(),
            s.getCategoryCode(), s.getStatus().name(), s.getApplicationDeadline(), s.getPassMark(),
            s.getValidityMonths(), criteriaCount, s.getPublishedAt());
    }

    public static CriterionDto toDto(EnlistmentCriterion c) {
        return new CriterionDto(c.getId(), c.getCode(), c.getName(), c.getWeight(),
            c.isMandatory());
    }

    public static ApplicationDto toDto(EnlistmentApplication a) {
        return new ApplicationDto(a.getId(), a.getSchedule().getCode(), a.getSupplierCode(),
            a.getStatus().name(), a.getScore(), a.getDecisionReason(), a.getSubmittedAt(),
            a.getDecidedAt());
    }

    public static AssessmentDto toDto(CriterionAssessment a) {
        return new AssessmentDto(a.getId(), a.getCriterion().getCode(), a.getCriterion().getName(),
            a.getCriterion().isMandatory(), a.getScore(), a.isMet(), a.getComment());
    }

    public static EnlistmentDto toDto(Enlistment e) {
        return new EnlistmentDto(e.getId(), e.getSupplierCode(), e.getCategoryCode(),
            e.getScheduleCode(), e.getValidFrom(), e.getValidUntil(), e.effectiveStatus(),
            e.isCurrentlyValid(), e.getRevokedReason());
    }
}
