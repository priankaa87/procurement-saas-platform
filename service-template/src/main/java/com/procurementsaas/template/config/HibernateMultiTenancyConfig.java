package com.procurementsaas.template.config;

import com.procurementsaas.template.tenancy.CurrentTenantResolver;
import com.procurementsaas.template.tenancy.TenantConnectionProvider;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the schema-per-tenant resolver and connection provider with Hibernate.
 */
@Configuration
public class HibernateMultiTenancyConfig {

    @Bean
    public HibernatePropertiesCustomizer multiTenancyCustomizer(
            TenantConnectionProvider connectionProvider,
            CurrentTenantResolver tenantResolver) {
        return props -> {
            props.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
            props.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantResolver);
        };
    }
}
