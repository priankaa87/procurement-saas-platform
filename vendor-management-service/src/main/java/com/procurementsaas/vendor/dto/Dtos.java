package com.procurementsaas.vendor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.Set;

/** Request/response payloads for the Vendor Management API. */
public final class Dtos {

    private Dtos() {
    }

    public record SupplierDto(Long id, String code, String name, String legalName, String email,
                              String phone, String taxId, String status, String countryIso2,
                              Set<String> categoryCodes) {
    }

    public record CreateSupplierRequest(
        @NotBlank String code,
        @NotBlank String name,
        String legalName,
        @Email String email,
        String phone,
        String taxId,
        String countryIso2,
        Set<String> categoryCodes) {
    }

    public record ContactDto(Long id, String name, String email, String phone, boolean primaryContact) {
    }

    public record CreateContactRequest(
        @NotBlank String name,
        @Email String email,
        String phone,
        boolean primaryContact) {
    }

    public record DocumentDto(Long id, String docType, String fileName, String storageKey,
                              LocalDate expiresAt, boolean expired) {
    }

    public record CreateDocumentRequest(
        @NotBlank String docType,
        @NotBlank String fileName,
        @NotBlank String storageKey,
        LocalDate expiresAt) {
    }

    public record DebarmentDto(Long id, String reason, LocalDate debarredFrom,
                               LocalDate debarredUntil, boolean active) {
    }

    public record DebarRequest(
        @NotBlank String reason,
        LocalDate debarredUntil) {
    }
}
