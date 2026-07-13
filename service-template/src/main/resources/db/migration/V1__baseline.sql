-- Baseline migration for the service-template schema.
-- Each business service replaces this with its own domain tables.
-- For schema-per-tenant, this script is applied once per provisioned tenant schema.

CREATE TABLE IF NOT EXISTS service_info (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    version     VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO service_info (name, version) VALUES ('service-template', '0.1.0');
