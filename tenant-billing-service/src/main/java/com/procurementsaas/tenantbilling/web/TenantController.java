package com.procurementsaas.tenantbilling.web;

import com.procurementsaas.tenantbilling.dto.Dtos.ChangePlanRequest;
import com.procurementsaas.tenantbilling.dto.Dtos.EntitlementsDto;
import com.procurementsaas.tenantbilling.dto.Dtos.OnboardTenantRequest;
import com.procurementsaas.tenantbilling.dto.Dtos.RecordUsageRequest;
import com.procurementsaas.tenantbilling.dto.Dtos.TenantDto;
import com.procurementsaas.tenantbilling.service.TenantService;
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

/**
 * Tenant administration. These are platform-operator actions, guarded by control-plane
 * privileges that no tenant user should ever hold.
 */
@RestController
@RequestMapping("/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FEATURE_TENANT_VIEW')")
    public List<TenantDto> list() {
        return tenantService.list();
    }

    @GetMapping("/{tenantKey}")
    @PreAuthorize("hasAuthority('FEATURE_TENANT_VIEW')")
    public TenantDto get(@PathVariable String tenantKey) {
        return tenantService.get(tenantKey);
    }

    /** Registers the tenant, provisions its schema, and activates it. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEATURE_TENANT_MANAGE')")
    public TenantDto onboard(@Valid @RequestBody OnboardTenantRequest request) {
        return tenantService.onboard(request);
    }

    @PostMapping("/{tenantKey}/suspend")
    @PreAuthorize("hasAuthority('FEATURE_TENANT_MANAGE')")
    public TenantDto suspend(@PathVariable String tenantKey) {
        return tenantService.suspend(tenantKey);
    }

    @PostMapping("/{tenantKey}/reactivate")
    @PreAuthorize("hasAuthority('FEATURE_TENANT_MANAGE')")
    public TenantDto reactivate(@PathVariable String tenantKey) {
        return tenantService.reactivate(tenantKey);
    }

    @PostMapping("/{tenantKey}/cancel")
    @PreAuthorize("hasAuthority('FEATURE_TENANT_MANAGE')")
    public TenantDto cancel(@PathVariable String tenantKey) {
        return tenantService.cancel(tenantKey);
    }

    @PutMapping("/{tenantKey}/plan")
    @PreAuthorize("hasAuthority('FEATURE_TENANT_MANAGE')")
    public TenantDto changePlan(@PathVariable String tenantKey,
                                @Valid @RequestBody ChangePlanRequest request) {
        return tenantService.changePlan(tenantKey, request);
    }

    @GetMapping("/{tenantKey}/entitlements")
    @PreAuthorize("hasAuthority('FEATURE_TENANT_VIEW')")
    public EntitlementsDto entitlements(@PathVariable String tenantKey) {
        return tenantService.entitlements(tenantKey);
    }

    /** Rejected with 409 once the plan's limit for the metric is reached. */
    @PostMapping("/{tenantKey}/usage")
    @PreAuthorize("hasAuthority('FEATURE_TENANT_MANAGE')")
    public EntitlementsDto recordUsage(@PathVariable String tenantKey,
                                       @Valid @RequestBody RecordUsageRequest request) {
        return tenantService.recordUsage(tenantKey, request);
    }
}
