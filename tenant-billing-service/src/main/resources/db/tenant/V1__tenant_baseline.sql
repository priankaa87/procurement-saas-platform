-- Baseline applied inside every newly provisioned TENANT schema.
--
-- In production each service contributes its own migrations here (or runs them against the
-- new schema on first use), so a tenant schema ends up holding the tender, vendor,
-- evaluation and notification tables. This baseline establishes the schema and records
-- what it was provisioned with.

CREATE TABLE tenant_info (
    id             BIGSERIAL PRIMARY KEY,
    tenant_key     VARCHAR(30)  NOT NULL,
    provisioned_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    schema_version VARCHAR(20)  NOT NULL
);

INSERT INTO tenant_info (tenant_key, schema_version)
VALUES (current_schema(), '1.0');
