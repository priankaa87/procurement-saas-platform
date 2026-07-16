package com.procurementsaas.contract.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Request/response payloads for the Award &amp; Contract API. */
public final class Dtos {

    private Dtos() {
    }

    public record AwardDto(Long id, String tenderCode, String tenderTitle, String supplierCode,
                           BigDecimal amount, String currencyCode, String status,
                           Instant issuedAt, LocalDate respondBy, Instant respondedAt,
                           String declineReason) {
    }

    /** Manual issuance, for awards not raised from a tender event. */
    public record IssueAwardRequest(
        @NotBlank String tenderCode,
        String tenderTitle,
        @NotBlank String supplierCode,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal amount,
        @NotBlank String currencyCode,
        LocalDate respondBy) {
    }

    public record DeclineRequest(@NotBlank String reason) {
    }

    public record WorkOrderDto(Long id, String code, String tenderCode, String supplierCode,
                               String status, BigDecimal totalAmount, String currencyCode,
                               Instant issuedAt, Instant completedAt, long lineCount) {
    }

    public record CreateWorkOrderRequest(@NotBlank String code) {
    }

    public record DeliveryLineDto(Long id, int lineNo, String itemCode, BigDecimal orderedQuantity,
                                  BigDecimal receivedQuantity, BigDecimal outstandingQuantity,
                                  String unitCode, LocalDate dueDate, String status,
                                  boolean overdue) {
    }

    public record AddLineRequest(
        @Min(1) int lineNo,
        @NotBlank String itemCode,
        @NotNull @DecimalMin(value = "0.001") BigDecimal orderedQuantity,
        @NotBlank String unitCode,
        @NotNull LocalDate dueDate) {
    }

    public record ReceiveRequest(
        @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
        @NotBlank String receivedBy,
        String remarks) {
    }

    public record ExtendRequest(@NotNull LocalDate newDueDate) {
    }

    public record GoodsReceiptDto(Long id, int lineNo, BigDecimal quantity, String receivedBy,
                                  Instant receivedAt, boolean late, String remarks) {
    }
}
