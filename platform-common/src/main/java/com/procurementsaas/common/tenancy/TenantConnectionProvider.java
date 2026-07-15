package com.procurementsaas.common.tenancy;

import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Schema-per-tenant connection provider. A single physical {@link DataSource} backs all
 * tenants; before handing a connection to Hibernate we switch the active PostgreSQL schema,
 * and reset it on release so pooled connections stay clean.
 *
 * <p>Only simple identifiers are accepted, so a hostile tenant id cannot inject SQL or
 * escape into another schema; anything unexpected falls back to the default schema.
 */
public class TenantConnectionProvider extends AbstractMultiTenantConnectionProvider<String> {

    private final DataSource dataSource;

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
        Connection connection = getAnyConnectionProvider().getConnection();
        connection.setSchema(sanitize(tenantIdentifier));
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        connection.setSchema(TenantContext.DEFAULT_TENANT);
        getAnyConnectionProvider().closeConnection(connection);
    }

    private static String sanitize(String tenant) {
        if (tenant == null || !tenant.matches("[A-Za-z0-9_]{1,63}")) {
            return TenantContext.DEFAULT_TENANT;
        }
        return tenant;
    }

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
