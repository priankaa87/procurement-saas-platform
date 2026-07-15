package com.procurementsaas.common.autoconfigure;

import com.procurementsaas.common.tenancy.CurrentTenantResolver;
import com.procurementsaas.common.tenancy.TenantConnectionProvider;
import com.procurementsaas.common.tenancy.TenantFilter;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import javax.sql.DataSource;

/**
 * Wires schema-per-tenant multi-tenancy into any service on the classpath that has a
 * {@link DataSource}: the request filter that resolves the tenant, and the Hibernate
 * resolver/connection provider that switch schema per request.
 */
@AutoConfiguration
@ConditionalOnClass(AvailableSettings.class)
public class PlatformTenancyAutoConfiguration {

    /** Runs early so the tenant is bound before anything touches the database. */
    @Bean
    @ConditionalOnMissingBean
    public FilterRegistrationBean<TenantFilter> tenantFilterRegistration() {
        FilterRegistrationBean<TenantFilter> registration =
            new FilterRegistrationBean<>(new TenantFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    public CurrentTenantResolver currentTenantResolver() {
        return new CurrentTenantResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(DataSource.class)
    public TenantConnectionProvider tenantConnectionProvider(DataSource dataSource) {
        return new TenantConnectionProvider(dataSource);
    }

    @Bean
    @ConditionalOnBean({TenantConnectionProvider.class, CurrentTenantResolver.class})
    public HibernatePropertiesCustomizer multiTenancyCustomizer(
            TenantConnectionProvider connectionProvider,
            CurrentTenantResolver tenantResolver) {
        return props -> {
            props.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
            props.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantResolver);
        };
    }
}
