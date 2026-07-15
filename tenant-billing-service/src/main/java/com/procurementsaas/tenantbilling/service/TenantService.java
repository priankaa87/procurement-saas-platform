package com.procurementsaas.tenantbilling.service;

import com.procurementsaas.common.web.NotFoundException;
import com.procurementsaas.tenantbilling.domain.Plan;
import com.procurementsaas.tenantbilling.domain.Tenant;
import com.procurementsaas.tenantbilling.domain.UsageRecord;
import com.procurementsaas.tenantbilling.dto.Dtos.ChangePlanRequest;
import com.procurementsaas.tenantbilling.dto.Dtos.EntitlementDto;
import com.procurementsaas.tenantbilling.dto.Dtos.EntitlementsDto;
import com.procurementsaas.tenantbilling.dto.Dtos.OnboardTenantRequest;
import com.procurementsaas.tenantbilling.dto.Dtos.RecordUsageRequest;
import com.procurementsaas.tenantbilling.dto.Dtos.TenantDto;
import com.procurementsaas.tenantbilling.provisioning.TenantProvisioner;
import com.procurementsaas.tenantbilling.repo.PlanRepository;
import com.procurementsaas.tenantbilling.repo.TenantRepository;
import com.procurementsaas.tenantbilling.repo.UsageRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Tenant onboarding, lifecycle, entitlements, and usage metering. */
@Service
@Transactional
public class TenantService {

    /** Metrics a plan can cap. */
    private static final List<String> METERED = List.of("users", "tenders");

    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final UsageRecordRepository usageRepository;
    private final TenantProvisioner provisioner;

    public TenantService(TenantRepository tenantRepository, PlanRepository planRepository,
                         UsageRecordRepository usageRepository, TenantProvisioner provisioner) {
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.usageRepository = usageRepository;
        this.provisioner = provisioner;
    }

    @Transactional(readOnly = true)
    public List<TenantDto> list() {
        return tenantRepository.findAll().stream().map(TenantMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public TenantDto get(String tenantKey) {
        return TenantMapper.toDto(findTenant(tenantKey));
    }

    /**
     * Self-service onboarding: register the tenant, provision its schema, activate.
     *
     * <p>The schema is created before the tenant is marked ACTIVE, so a tenant is never
     * advertised as usable while its database is missing. If provisioning throws, the whole
     * transaction rolls back and no half-made tenant is left in the registry — though the
     * schema itself is not transactional, which is why provisioning is idempotent.
     */
    public TenantDto onboard(OnboardTenantRequest request) {
        if (tenantRepository.existsByTenantKey(request.tenantKey())) {
            throw new IllegalArgumentException("Tenant key already taken: " + request.tenantKey());
        }
        Plan plan = findPlan(request.planCode());

        // Constructor validates the key — it is about to become a schema name.
        Tenant tenant = new Tenant(request.tenantKey(), request.name(), plan.getCode(),
            request.contactEmail());

        provisioner.provision(tenant.getSchemaName());
        tenant.markProvisioned();

        return TenantMapper.toDto(tenantRepository.save(tenant));
    }

    public TenantDto suspend(String tenantKey) {
        Tenant tenant = findTenant(tenantKey);
        tenant.suspend();
        return TenantMapper.toDto(tenantRepository.save(tenant));
    }

    public TenantDto reactivate(String tenantKey) {
        Tenant tenant = findTenant(tenantKey);
        tenant.reactivate();
        return TenantMapper.toDto(tenantRepository.save(tenant));
    }

    public TenantDto changePlan(String tenantKey, ChangePlanRequest request) {
        Tenant tenant = findTenant(tenantKey);
        Plan plan = findPlan(request.planCode());
        tenant.changePlan(plan.getCode());
        return TenantMapper.toDto(tenantRepository.save(tenant));
    }

    /**
     * Cancels a tenant. The schema is deliberately left intact: cancellation is a billing
     * decision, and deleting a customer's data the moment they stop paying is how you lose
     * it for the ones who come back or who need it for an audit. Deletion is a separate,
     * explicit act.
     */
    public TenantDto cancel(String tenantKey) {
        Tenant tenant = findTenant(tenantKey);
        tenant.cancel();
        return TenantMapper.toDto(tenantRepository.save(tenant));
    }

    // --- Metering & entitlements ---------------------------------------------

    /**
     * Records usage, refusing to exceed the plan's limit.
     *
     * @throws IllegalStateException if the tenant is not usable, or the plan's limit for
     *                               this metric would be breached (surfaced as 409)
     */
    public EntitlementsDto recordUsage(String tenantKey, RecordUsageRequest request) {
        Tenant tenant = findTenant(tenantKey);
        if (!tenant.isUsable()) {
            throw new IllegalStateException(
                "Tenant is not active (status " + tenant.getStatus() + "): " + tenantKey);
        }
        Plan plan = findPlan(tenant.getPlanCode());

        int limit = plan.limitFor(request.metric());
        if (limit >= 0) {
            long used = usageRepository.totalFor(tenant.getId(), request.metric());
            if (used + request.quantity() > limit) {
                throw new IllegalStateException(
                    "Plan " + plan.getCode() + " allows " + limit + " " + request.metric()
                        + "; " + used + " already used and " + request.quantity() + " requested");
            }
        }
        usageRepository.save(new UsageRecord(tenant, request.metric(), request.quantity()));
        return entitlements(tenantKey);
    }

    @Transactional(readOnly = true)
    public EntitlementsDto entitlements(String tenantKey) {
        Tenant tenant = findTenant(tenantKey);
        Plan plan = findPlan(tenant.getPlanCode());

        List<EntitlementDto> entitlements = METERED.stream().map(metric -> {
            long used = usageRepository.totalFor(tenant.getId(), metric);
            int limit = plan.limitFor(metric);
            boolean unlimited = limit < 0;
            long remaining = unlimited ? Long.MAX_VALUE : Math.max(0, limit - used);
            return new EntitlementDto(metric, used, limit, unlimited, remaining);
        }).toList();

        return new EntitlementsDto(tenant.getTenantKey(), plan.getCode(), entitlements);
    }

    Tenant findTenant(String tenantKey) {
        return tenantRepository.findByTenantKey(tenantKey)
            .orElseThrow(() -> new NotFoundException("Tenant not found: " + tenantKey));
    }

    private Plan findPlan(String planCode) {
        return planRepository.findByCode(planCode)
            .orElseThrow(() -> new NotFoundException("Plan not found: " + planCode));
    }
}
