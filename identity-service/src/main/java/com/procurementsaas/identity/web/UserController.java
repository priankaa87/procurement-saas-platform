package com.procurementsaas.identity.web;

import com.procurementsaas.identity.dto.Dtos.AssignRolesRequest;
import com.procurementsaas.identity.dto.Dtos.CreateUserRequest;
import com.procurementsaas.identity.dto.Dtos.UserDto;
import com.procurementsaas.identity.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FEATURE_USER_VIEW')")
    public List<UserDto> list() {
        return userService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FEATURE_USER_VIEW')")
    public UserDto get(@PathVariable Long id) {
        return userService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_USER_MANAGE')")
    public UserDto create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('FEATURE_USER_MANAGE')")
    public UserDto assignRoles(@PathVariable Long id, @RequestBody AssignRolesRequest request) {
        return userService.assignRoles(id, request.roleCodes());
    }
}
