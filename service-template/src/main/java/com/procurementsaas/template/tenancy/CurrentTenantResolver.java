package com.procurementsaas.template.tenancy;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Tells Hibernate which tenant (schema) the current operation belongs to, by reading
 * {@link TenantContext}. Combined with {@link TenantConnectionProvider}, this implements
 * the schema-per-tenant strategy.
 */
@Component
public class CurrentTenantResolver implements CurrentTenantIdentifierResolver<String> {

    @Override
    public String resolveCurrentTenantIdentifier() {
        return TenantContext.getTenant();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
