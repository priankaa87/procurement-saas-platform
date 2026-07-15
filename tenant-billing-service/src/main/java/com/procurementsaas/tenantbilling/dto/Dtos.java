package com.procurementsaas.tenantbilling.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request/response payloads for the Tenant &amp; Billing API. */
public final class Dtos {

    private Dtos() {
    }

    public record TenantDto(Long id, String tenantKey, String name, String schemaName,
                            String status, String planCode, String contactEmail,
                            Instant createdAt, Instant activatedAt) {
    }

    public record OnboardTenantRequest(
        @NotBlank String tenantKey,
        @NotBlank String name,
        @NotBlank String planCode,
        @Email String contactEmail) {
    }

    public record ChangePlanRequest(@NotBlank String planCode) {
    }

    public record PlanDto(Long id, String code, String name, BigDecimal priceMonthly,
                          String currencyCode, int maxUsers, int maxTenders) {
    }

    public record RecordUsageRequest(
        @NotBlank String metric,
        @Min(1) long quantity) {
    }

    /** What a tenant is entitled to, and how much of it they have used. */
    public record EntitlementDto(String metric, long used, int limit, boolean unlimited,
                                 long remaining) {
    }

    public record EntitlementsDto(String tenantKey, String planCode, List<EntitlementDto> entitlements) {
    }

    public record InvoiceDto(Long id, String tenantKey, String planCode, LocalDate periodStart,
                             LocalDate periodEnd, BigDecimal amount, String currencyCode,
                             String status, Instant issuedAt, Instant paidAt) {
    }

    public record GenerateInvoiceRequest(LocalDate periodStart) {
    }
}
