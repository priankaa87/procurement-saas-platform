package com.procurementsaas.notification.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/** Request/response payloads for the Notification API. */
public final class Dtos {

    private Dtos() {
    }

    public record NotificationDto(Long id, String recipient, String templateCode, String subject,
                                  String body, String status, String error, Instant createdAt,
                                  Instant sentAt) {
    }

    public record TemplateDto(Long id, String code, String subject, String body) {
    }

    public record UpdateTemplateRequest(@NotBlank String subject, @NotBlank String body) {
    }
}
