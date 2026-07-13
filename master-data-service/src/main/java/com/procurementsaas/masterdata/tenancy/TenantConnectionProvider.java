package com.procurementsaas.masterdata.tenancy;

import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Schema-per-tenant connection provider. Switches the active PostgreSQL schema before
 * handing a connection to Hibernate and resets it on release. Only known, provisioned
 * tenant schemas are allowed, to prevent injection via the tenant identifier.
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
