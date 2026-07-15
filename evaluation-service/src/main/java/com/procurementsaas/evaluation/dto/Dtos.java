package com.procurementsaas.evaluation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/** Request/response payloads for the Evaluation API. */
public final class Dtos {

    private Dtos() {
    }

    public record EvaluationDto(Long id, String tenderCode, String status, int technicalWeight,
                                int financialWeight, BigDecimal passMark) {
    }

    public record CreateEvaluationRequest(
        @NotBlank String tenderCode,
        @Min(0) int technicalWeight,
        @Min(0) int financialWeight,
        @NotNull BigDecimal passMark) {
    }

    public record CriterionDto(Long id, String code, String name, int weight) {
    }

    public record AddCriterionRequest(
        @NotBlank String code,
        @NotBlank String name,
        @Min(1) int weight) {
    }

    public record ParticipantDto(Long id, String supplierCode, BigDecimal bidAmount,
                                 BigDecimal technicalScore, BigDecimal financialScore,
                                 BigDecimal combinedScore, Boolean qualified, Integer rank) {
    }

    public record AddParticipantRequest(
        @NotBlank String supplierCode,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal bidAmount) {
    }

    public record ScoreRequest(
        @NotBlank String supplierCode,
        @NotBlank String criterionCode,
        @NotNull BigDecimal score,
        String comment) {
    }

    public record ScoreDto(Long id, String supplierCode, String criterionCode, BigDecimal score,
                           String comment) {
    }

    /** A single row of the comparative statement. */
    public record ComparativeRow(Integer rank, String supplierCode, BigDecimal bidAmount,
                                 BigDecimal technicalScore, BigDecimal financialScore,
                                 BigDecimal combinedScore, boolean qualified) {
    }

    public record ComparativeStatementDto(String tenderCode, String status, int technicalWeight,
                                          int financialWeight, BigDecimal passMark,
                                          String recommendedSupplierCode,
                                          List<ComparativeRow> rows) {
    }
}
