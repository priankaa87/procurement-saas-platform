package com.procurementsaas.identity.service;

import com.procurementsaas.identity.domain.AppUser;
import com.procurementsaas.identity.dto.Dtos.CreateUserRequest;
import com.procurementsaas.identity.dto.Dtos.UserDto;
import com.procurementsaas.identity.repo.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class UserService {

    private final AppUserRepository userRepository;
    private final RoleService roleService;

    public UserService(AppUserRepository userRepository, RoleService roleService) {
        this.userRepository = userRepository;
        this.roleService = roleService;
    }

    @Transactional(readOnly = true)
    public List<UserDto> list() {
        return userRepository.findAll().stream().map(IdentityMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public UserDto get(Long id) {
        return IdentityMapper.toDto(findUser(id));
    }

    public UserDto create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists: " + request.username());
        }
        AppUser user = new AppUser(request.username(), request.email(), request.fullName());
        user.setRoles(roleService.resolveByCodes(request.roleCodes()));
        return IdentityMapper.toDto(userRepository.save(user));
    }

    public UserDto assignRoles(Long id, Set<String> roleCodes) {
        AppUser user = findUser(id);
        user.setRoles(roleService.resolveByCodes(roleCodes));
        return IdentityMapper.toDto(userRepository.save(user));
    }

    private AppUser findUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }
}
