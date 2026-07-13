package com.procurementsaas.identity.service;

import com.procurementsaas.identity.domain.AppUser;
import com.procurementsaas.identity.domain.Feature;
import com.procurementsaas.identity.domain.Role;
import com.procurementsaas.identity.dto.Dtos.FeatureDto;
import com.procurementsaas.identity.dto.Dtos.RoleDto;
import com.procurementsaas.identity.dto.Dtos.UserDto;

import java.util.Set;
import java.util.stream.Collectors;

/** Maps JPA entities to API DTOs. */
public final class IdentityMapper {

    private IdentityMapper() {
    }

    public static FeatureDto toDto(Feature f) {
        return new FeatureDto(f.getId(), f.getCode(), f.getName(), f.getModule());
    }

    public static RoleDto toDto(Role r) {
        Set<String> features = r.getFeatures().stream()
            .map(Feature::getCode)
            .collect(Collectors.toCollection(java.util.TreeSet::new));
        return new RoleDto(r.getId(), r.getCode(), r.getName(), r.getDescription(), features);
    }

    public static UserDto toDto(AppUser u) {
        Set<String> roles = u.getRoles().stream()
            .map(Role::getCode)
            .collect(Collectors.toCollection(java.util.TreeSet::new));
        return new UserDto(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(),
            u.isEnabled(), roles);
    }
}
