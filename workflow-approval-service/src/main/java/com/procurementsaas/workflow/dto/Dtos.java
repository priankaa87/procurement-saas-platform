package com.procurementsaas.workflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request/response payloads for the Workflow &amp; Approval API. */
public final class Dtos {

    private Dtos() {
    }

    public record WorkflowDto(Long id, String code, String name, String subjectType,
                              boolean active, List<StepDto> steps) {
    }

    public record StepDto(Long id, int stepNo, String name, String roleCode) {
    }

    public record CreateWorkflowRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String subjectType) {
    }

    public record AddStepRequest(
        @Min(1) int stepNo,
        @NotBlank String name,
        @NotBlank String roleCode) {
    }

    public record RequestDto(Long id, String workflowCode, String subjectType, String subjectRef,
                             String requestedBy, String reason, String status, int currentStep,
                             int totalSteps, String currentRoleCode, Instant createdAt,
                             Instant completedAt) {
    }

    public record StartRequest(
        @NotBlank String workflowCode,
        @NotBlank String subjectRef,
        String reason) {
    }

    public record DecideRequest(String comment) {
    }

    public record ActionDto(Long id, int stepNo, String roleCode, String actorId,
                            String onBehalfOf, boolean delegated, String decision,
                            String comment, Instant actedAt) {
    }

    public record DelegationDto(Long id, String fromUser, String toUser, String roleCode,
                                LocalDate validFrom, LocalDate validTo, boolean revoked,
                                boolean currentlyValid, String reason) {
    }

    public record CreateDelegationRequest(
        @NotBlank String toUser,
        @NotBlank String roleCode,
        @NotNull LocalDate validFrom,
        @NotNull LocalDate validTo,
        String reason) {
    }
}
