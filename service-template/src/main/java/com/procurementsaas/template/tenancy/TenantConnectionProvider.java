package com.procurementsaas.template.tenancy;

import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Schema-per-tenant connection provider. A single physical {@link DataSource} backs all
 * tenants; before handing a connection to Hibernate we switch the active PostgreSQL schema.
 * Resetting to {@code public} on release keeps pooled connections clean.
 *
 * <p>Only known/provisioned tenant schemas should be allowed (provisioned by the Tenant &amp;
 * Billing service) to prevent injection via the tenant identifier.
 */
@Component
public class TenantConnectionProvider extends AbstractMultiTenantConnectionProvider<String> {

    private final DataSource dataSource;

    @Autowired
    public TenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected ConnectionProvider getAnyConnectionProvider() {
        return new SingleDataSourceConnectionProvider(dataSource);
    }

    @Override
    protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
        return getAnyConnectionProvider();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = super.getAnyConnectionProvider().getConnection();
        connection.setSchema(sanitize(tenantIdentifier));
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        connection.setSchema(TenantContext.DEFAULT_TENANT);
        super.getAnyConnectionProvider().closeConnection(connection);
    }

    /** Only allow simple identifiers to avoid search_path / schema injection. */
    private static String sanitize(String tenant) {
        if (tenant == null || !tenant.matches("[A-Za-z0-9_]{1,63}")) {
            return TenantContext.DEFAULT_TENANT;
        }
        return tenant;
    }

    /** Minimal ConnectionProvider wrapping a Spring-managed DataSource. */
    static final class SingleDataSourceConnectionProvider implements ConnectionProvider {
        private final DataSource ds;
        SingleDataSourceConnectionProvider(DataSource ds) { this.ds = ds; }

        @Override public Connection getConnection() throws SQLException { return ds.getConnection(); }
        @Override public void closeConnection(Connection conn) throws SQLException { conn.close(); }
        @Override public boolean supportsAggressiveRelease() { return false; }
        @Override public boolean isUnwrappableAs(Class<?> unwrapType) { return false; }
        @Override public <T> T unwrap(Class<T> unwrapType) { return null; }
    }
}
