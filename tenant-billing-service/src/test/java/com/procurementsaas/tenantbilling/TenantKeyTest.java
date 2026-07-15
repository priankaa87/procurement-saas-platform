package com.procurementsaas.tenantbilling;

import com.procurementsaas.tenantbilling.domain.TenantKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for tenant-key validation.
 *
 * <p>This is a security boundary: the key ends up inside DDL as a schema name, so these
 * tests are about keeping hostile or ambiguous input away from the database.
 */
class TenantKeyTest {

    @ParameterizedTest
    @ValueSource(strings = {"acme", "acme_corp", "a1b2c3", "abc", "brac_ltd"})
    void ordinaryKeysAreAccepted(String key) {
        assertThatCode(() -> TenantKey.validate(key)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "ab",                       // too short
        "1acme",                    // must start with a letter
        "Acme",                     // uppercase
        "acme-corp",                // hyphen
        "acme corp",                // space
        "acme;drop",                // statement separator
        "acme\"",                   // quote — would break out of the identifier
        "acme--",                   // comment marker
        "a".repeat(31)              // too long
    })
    void hostileOrMalformedKeysAreRejected(String key) {
        assertThatThrownBy(() -> TenantKey.validate(key))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aNullKeyIsRejected() {
        assertThatThrownBy(() -> TenantKey.validate(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "information_schema", "pg_catalog", "postgres", "admin"})
    void reservedNamesAreRejected(String key) {
        // A tenant called "public" would point at the shared schema.
        assertThatThrownBy(() -> TenantKey.validate(key))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reserved");
    }

    @Test
    void schemaNamesAreAlwaysPrefixed() {
        // The prefix is why a valid key can never collide with a built-in schema.
        assertThat(TenantKey.schemaFor("acme")).isEqualTo("tenant_acme");
    }

    @Test
    void anInvalidKeyNeverProducesASchemaName() {
        assertThatThrownBy(() -> TenantKey.schemaFor("public"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
