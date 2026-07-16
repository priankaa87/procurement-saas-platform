package com.procurementsaas.reporting.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.Map;

/** Request/response payloads for the Reporting API. */
public final class Dtos {

    private Dtos() {
    }

    public record DefinitionDto(Long id, String code, String name, String description,
                                String format, boolean active) {
    }

    public record RunReportRequest(
        @NotBlank String definitionCode,
        Map<String, String> parameters) {
    }

    public record JobDto(Long id, String definitionCode, String status, String requestedBy,
                         Integer rowCount, Long sizeBytes, String error, Instant createdAt,
                         Instant completedAt, boolean downloadable) {
    }
}
