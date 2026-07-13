package com.procurementsaas.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

/**
 * Request/response payloads for the Identity API, grouped as records in one file for
 * brevity. Each service returns DTOs, never JPA entities.
 */
public final class Dtos {

    private Dtos() {
    }

    public record FeatureDto(Long id, String code, String name, String module) {
    }

    public record RoleDto(Long id, String code, String name, String description, Set<String> features) {
    }

    public record UserDto(Long id, String username, String email, String fullName,
                          boolean enabled, Set<String> roles) {
    }

    public record CreateUserRequest(
        @NotBlank String username,
        @Email String email,
        String fullName,
        Set<String> roleCodes) {
    }

    public record CreateRoleRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        Set<String> featureCodes) {
    }

    public record AssignRolesRequest(Set<String> roleCodes) {
    }
}
