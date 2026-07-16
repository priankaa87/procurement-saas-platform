package com.procurementsaas.workflow.service;

import com.procurementsaas.workflow.domain.ApprovalAction;
import com.procurementsaas.workflow.domain.ApprovalRequest;
import com.procurementsaas.workflow.domain.ApprovalStep;
import com.procurementsaas.workflow.domain.ApprovalWorkflow;
import com.procurementsaas.workflow.domain.Delegation;
import com.procurementsaas.workflow.dto.Dtos.ActionDto;
import com.procurementsaas.workflow.dto.Dtos.DelegationDto;
import com.procurementsaas.workflow.dto.Dtos.RequestDto;
import com.procurementsaas.workflow.dto.Dtos.StepDto;
import com.procurementsaas.workflow.dto.Dtos.WorkflowDto;

import java.time.LocalDate;
import java.util.List;

/** Maps workflow entities to API DTOs. */
public final class WorkflowMapper {

    private WorkflowMapper() {
    }

    public static WorkflowDto toDto(ApprovalWorkflow w, List<ApprovalStep> steps) {
        return new WorkflowDto(w.getId(), w.getCode(), w.getName(), w.getSubjectType(),
            w.isActive(), steps.stream().map(WorkflowMapper::toDto).toList());
    }

    public static StepDto toDto(ApprovalStep s) {
        return new StepDto(s.getId(), s.getStepNo(), s.getName(), s.getRoleCode());
    }

    /** {@code currentRoleCode} tells a caller who the request is waiting on. */
    public static RequestDto toDto(ApprovalRequest r, String currentRoleCode) {
        return new RequestDto(r.getId(), r.getWorkflowCode(), r.getSubjectType(), r.getSubjectRef(),
            r.getRequestedBy(), r.getReason(), r.getStatus().name(), r.getCurrentStep(),
            r.getTotalSteps(), currentRoleCode, r.getCreatedAt(), r.getCompletedAt());
    }

    public static ActionDto toDto(ApprovalAction a) {
        return new ActionDto(a.getId(), a.getStepNo(), a.getRoleCode(), a.getActorId(),
            a.getOnBehalfOf(), a.wasDelegated(), a.getDecision().name(), a.getComment(),
            a.getActedAt());
    }

    public static DelegationDto toDto(Delegation d) {
        return new DelegationDto(d.getId(), d.getFromUser(), d.getToUser(), d.getRoleCode(),
            d.getValidFrom(), d.getValidTo(), d.isRevoked(), d.isValidOn(LocalDate.now()),
            d.getReason());
    }
}
