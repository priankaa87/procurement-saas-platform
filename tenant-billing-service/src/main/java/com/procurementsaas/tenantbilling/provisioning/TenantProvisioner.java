package com.procurementsaas.tenantbilling.provisioning;

import com.procurementsaas.tenantbilling.domain.TenantKey;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates and migrates a tenant's PostgreSQL schema.
 *
 * <p>This is the mechanism the whole schema-per-tenant design rests on: a new customer
 * gets their own schema, and every service's tables are created inside it, so one tenant's
 * bid data is not one buggy {@code WHERE} clause away from another's.
 *
 * <p>The schema name is interpolated into DDL — PostgreSQL does not accept a bind
 * parameter for an identifier — so it is validated against a strict pattern first and
 * quoted second. {@link TenantKey} guarantees the prefix and character set; the quoting
 * here is belt and braces.
 *
 * <p>Provisioning is idempotent: {@code CREATE SCHEMA IF NOT EXISTS} plus Flyway's own
 * versioning mean a retried onboarding converges rather than failing.
 */
@Component
public class TenantProvisioner {

    private static final Logger log = LoggerFactory.getLogger(TenantProvisioner.class);

    /**
     * Migrations applied inside each tenant schema. In production each service contributes
     * its own tables here (or migrates on first use); this baseline proves the mechanism.
     */
    private static final String TENANT_MIGRATIONS = "classpath:db/tenant";

    private final DataSource dataSource;

    public TenantProvisioner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void provision(String schemaName) {
        requireSafeIdentifier(schemaName);
        createSchema(schemaName);
        migrate(schemaName);
        log.info("Provisioned tenant schema {}", schemaName);
    }

    /** Drops a tenant's schema and everything in it. Irreversible. */
    public void deprovision(String schemaName) {
        requireSafeIdentifier(schemaName);
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
            log.warn("Deprovisioned tenant schema {}", schemaName);
        } catch (SQLException ex) {
            throw new ProvisioningException("Failed to drop schema " + schemaName, ex);
        }
    }

    public boolean schemaExists(String schemaName) {
        requireSafeIdentifier(schemaName);
        try (Connection connection = dataSource.getConnection();
             var ps = connection.prepareStatement(
                 "SELECT 1 FROM information_schema.schemata WHERE schema_name = ?")) {
            ps.setString(1, schemaName);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new ProvisioningException("Failed to check schema " + schemaName, ex);
        }
    }

    private void createSchema(String schemaName) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS \"" + schemaName + "\"");
        } catch (SQLException ex) {
            throw new ProvisioningException("Failed to create schema " + schemaName, ex);
        }
    }

    private void migrate(String schemaName) {
        try {
            Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .defaultSchema(schemaName)
                .locations(TENANT_MIGRATIONS)
                .load()
                .migrate();
        } catch (RuntimeException ex) {
            throw new ProvisioningException("Failed to migrate schema " + schemaName, ex);
        }
    }

    /**
     * Last line of defence before a name reaches DDL. Anything that did not come from
     * {@link TenantKey#schemaFor(String)} is rejected outright.
     */
    private static void requireSafeIdentifier(String schemaName) {
        if (schemaName == null
            || !schemaName.startsWith(TenantKey.SCHEMA_PREFIX)
            || !schemaName.matches("^[a-z][a-z0-9_]{2,39}$")) {
            throw new IllegalArgumentException("Unsafe schema name: " + schemaName);
        }
    }

    /** Thrown when a tenant's schema cannot be created, migrated, or dropped. */
    public static class ProvisioningException extends RuntimeException {
        public ProvisioningException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
