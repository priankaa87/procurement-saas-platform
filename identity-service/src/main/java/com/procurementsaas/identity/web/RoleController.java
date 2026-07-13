package com.procurementsaas.identity.web;

import com.procurementsaas.identity.dto.Dtos.CreateRoleRequest;
import com.procurementsaas.identity.dto.Dtos.RoleDto;
import com.procurementsaas.identity.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FEATURE_ROLE_VIEW')")
    public List<RoleDto> list() {
        return roleService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FEATURE_ROLE_VIEW')")
    public RoleDto get(@PathVariable Long id) {
        return roleService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_ROLE_MANAGE')")
    public RoleDto create(@Valid @RequestBody CreateRoleRequest request) {
        return roleService.create(request);
    }
}
