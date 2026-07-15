package com.procurementsaas.tenantbilling;

import com.procurementsaas.common.autoconfigure.PlatformTenancyAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Tenant &amp; Billing Service — the SaaS control plane.
 *
 * <p>Owns the tenant registry, onboarding (including provisioning each tenant's database
 * schema), plans and their entitlements, usage metering, and invoicing.
 *
 * <p>Note the deliberate exclusion of {@link PlatformTenancyAutoConfiguration}: every
 * other service is tenant-scoped, but this one <em>manages</em> tenants, so its own data
 * is control-plane data. Leaving tenancy switched on would let a caller set
 * {@code X-Tenant-ID} and read the tenant registry from inside a tenant's own schema —
 * exactly backwards.
 */
@SpringBootApplication(exclude = PlatformTenancyAutoConfiguration.class)
public class TenantBillingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TenantBillingServiceApplication.class, args);
    }
}
