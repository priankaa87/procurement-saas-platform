package com.procurementsaas.common.tenancy;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * Tells Hibernate which tenant (schema) the current operation belongs to, by reading
 * {@link TenantContext}. Combined with {@link TenantConnectionProvider}, this implements
 * the schema-per-tenant strategy.
 */
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
