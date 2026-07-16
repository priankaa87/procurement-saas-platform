package com.procurementsaas.enlistment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Request/response payloads for the Enlistment API. */
public final class Dtos {

    private Dtos() {
    }

    public record ScheduleDto(Long id, String code, String title, String description,
                              String categoryCode, String status, Instant applicationDeadline,
                              BigDecimal passMark, int validityMonths, long criteriaCount,
                              Instant publishedAt) {
    }

    public record CreateScheduleRequest(
        @NotBlank String code,
        @NotBlank String title,
        String description,
        @NotBlank String categoryCode,
        @NotNull Instant applicationDeadline,
        @NotNull BigDecimal passMark,
        @Min(1) int validityMonths) {
    }

    public record CriterionDto(Long id, String code, String name, int weight, boolean mandatory) {
    }

    public record AddCriterionRequest(
        @NotBlank String code,
        @NotBlank String name,
        @Min(1) int weight,
        boolean mandatory) {
    }

    public record ApplicationDto(Long id, String scheduleCode, String supplierCode, String status,
                                 BigDecimal score, String decisionReason, Instant submittedAt,
                                 Instant decidedAt) {
    }

    public record ApplyRequest(@NotBlank String supplierCode) {
    }

    public record AssessRequest(
        @NotBlank String criterionCode,
        @NotNull BigDecimal score,
        boolean met,
        String comment) {
    }

    public record AssessmentDto(Long id, String criterionCode, String criterionName,
                                boolean mandatory, BigDecimal score, boolean met, String comment) {
    }

    public record EnlistmentDto(Long id, String supplierCode, String categoryCode,
                                String scheduleCode, LocalDate validFrom, LocalDate validUntil,
                                String status, boolean currentlyValid, String revokedReason) {
    }
}
