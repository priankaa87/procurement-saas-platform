package com.procurementsaas.tenantbilling.domain;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validation for a tenant key.
 *
 * <p>This is a security boundary, not cosmetics: the key becomes part of a PostgreSQL
 * schema name that is interpolated into DDL, and the schema a tenant's data is isolated in.
 * A key like {@code public} or {@code pg_catalog} would point a tenant at shared or system
 * data; a key containing a quote could break out of the identifier entirely.
 *
 * <p>Two defences, both applied: the character set is restricted to lowercase letters,
 * digits and underscore, and every schema is prefixed with {@code tenant_} so it cannot
 * collide with a built-in schema even if this check were somehow bypassed.
 */
public final class TenantKey {

    private static final Pattern VALID = Pattern.compile("^[a-z][a-z0-9_]{2,29}$");

    /** Names that must never become a tenant, even with the prefix in place. */
    private static final Set<String> RESERVED = Set.of(
        "public", "information_schema", "pg_catalog", "pg_toast", "postgres",
        "admin", "system", "tenant", "template", "flyway");

    public static final String SCHEMA_PREFIX = "tenant_";

    private TenantKey() {
    }

    public static void validate(String key) {
        if (key == null || !VALID.matcher(key).matches()) {
            throw new IllegalArgumentException(
                "Tenant key must be 3-30 characters, start with a letter, and contain only "
                    + "lowercase letters, digits, or underscore: " + key);
        }
        if (RESERVED.contains(key)) {
            throw new IllegalArgumentException("Tenant key is reserved: " + key);
        }
    }

    /** The schema a tenant's data lives in. Always prefixed, never a bare key. */
    public static String schemaFor(String key) {
        validate(key);
        return SCHEMA_PREFIX + key;
    }
}
