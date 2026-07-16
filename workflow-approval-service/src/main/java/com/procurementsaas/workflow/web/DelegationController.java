package com.procurementsaas.workflow.web;

import com.procurementsaas.workflow.dto.Dtos.CreateDelegationRequest;
import com.procurementsaas.workflow.dto.Dtos.DelegationDto;
import com.procurementsaas.workflow.service.DelegationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lending and reclaiming authority. The delegator is always the authenticated caller —
 * you can only give away authority you are logged in as.
 */
@RestController
@RequestMapping("/delegations")
public class DelegationController {

    private final DelegationService delegationService;

    public DelegationController(DelegationService delegationService) {
        this.delegationService = delegationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_DELEGATION_MANAGE')")
    public DelegationDto create(@Valid @RequestBody CreateDelegationRequest request,
                                Authentication authentication) {
        return delegationService.create(CallerRoles.actorId(authentication),
            CallerRoles.of(authentication), request);
    }

    /** Authority this caller has lent out. */
    @GetMapping("/granted")
    @PreAuthorize("hasAuthority('FEATURE_DELEGATION_MANAGE')")
    public List<DelegationDto> granted(Authentication authentication) {
        return delegationService.granted(CallerRoles.actorId(authentication));
    }

    /** Authority lent to this caller. */
    @GetMapping("/received")
    @PreAuthorize("hasAuthority('FEATURE_DELEGATION_MANAGE')")
    public List<DelegationDto> received(Authentication authentication) {
        return delegationService.received(CallerRoles.actorId(authentication));
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('FEATURE_DELEGATION_MANAGE')")
    public DelegationDto revoke(@PathVariable Long id, Authentication authentication) {
        return delegationService.revoke(id, CallerRoles.actorId(authentication));
    }
}
