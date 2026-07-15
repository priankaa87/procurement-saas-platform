package com.procurementsaas.tender.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

/** Request/response payloads for the Tender API. */
public final class Dtos {

    private Dtos() {
    }

    public record TenderDto(Long id, String code, String title, String description, String status,
                            String currencyCode, Instant bidDeadline, Instant publishedAt,
                            Instant openedAt, String awardedSupplierCode, long itemCount) {
    }

    public record CreateTenderRequest(
        @NotBlank String code,
        @NotBlank String title,
        String description,
        @NotBlank String currencyCode,
        @NotNull Instant bidDeadline) {
    }

    public record TenderItemDto(Long id, String itemCode, String description,
                                BigDecimal quantity, String unitCode) {
    }

    public record AddItemRequest(
        @NotBlank String itemCode,
        String description,
        @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
        @NotBlank String unitCode) {
    }

    public record ParticipantDto(Long id, String supplierCode, String status, Instant invitedAt) {
    }

    public record InviteRequest(@NotBlank String supplierCode) {
    }

    public record BidDto(Long id, String supplierCode, BigDecimal totalAmount, String currencyCode,
                         String notes, Instant submittedAt) {
    }

    public record SubmitBidRequest(
        @NotBlank String supplierCode,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal totalAmount,
        String notes) {
    }

    /** Confirmation returned on submission — deliberately excludes any bid amounts. */
    public record BidReceiptDto(Long bidId, String supplierCode, Instant submittedAt,
                                String message) {
    }

    public record AwardRequest(@NotBlank String supplierCode) {
    }
}
