package com.procurementsaas.identity.service;

import com.procurementsaas.common.web.NotFoundException;
import com.procurementsaas.identity.domain.Feature;
import com.procurementsaas.identity.domain.Role;
import com.procurementsaas.identity.dto.Dtos.CreateRoleRequest;
import com.procurementsaas.identity.dto.Dtos.RoleDto;
import com.procurementsaas.identity.repo.FeatureRepository;
import com.procurementsaas.identity.repo.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final FeatureRepository featureRepository;

    public RoleService(RoleRepository roleRepository, FeatureRepository featureRepository) {
        this.roleRepository = roleRepository;
        this.featureRepository = featureRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleDto> list() {
        return roleRepository.findAll().stream().map(IdentityMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public RoleDto get(Long id) {
        return IdentityMapper.toDto(findRole(id));
    }

    public RoleDto create(CreateRoleRequest request) {
        if (roleRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Role code already exists: " + request.code());
        }
        Role role = new Role(request.code(), request.name(), request.description());
        role.setFeatures(resolveFeatures(request.featureCodes()));
        return IdentityMapper.toDto(roleRepository.save(role));
    }

    Role findRole(Long id) {
        return roleRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Role not found: " + id));
    }

    Set<Role> resolveByCodes(Set<String> codes) {
        Set<Role> roles = new HashSet<>();
        if (codes == null) {
            return roles;
        }
        for (String code : codes) {
            roles.add(roleRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Role not found: " + code)));
        }
        return roles;
    }

    private Set<Feature> resolveFeatures(Set<String> codes) {
        Set<Feature> features = new HashSet<>();
        if (codes == null) {
            return features;
        }
        for (String code : codes) {
            features.add(featureRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Feature not found: " + code)));
        }
        return features;
    }
}
