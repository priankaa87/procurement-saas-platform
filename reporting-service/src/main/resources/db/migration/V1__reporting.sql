-- Reporting schema: report catalogue and the jobs that produce files from it.
-- Applied per provisioned tenant schema (schema-per-tenant).

CREATE TABLE report_definition (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(60)  NOT NULL UNIQUE,
    name          VARCHAR(200) NOT NULL,
    description   VARCHAR(500),
    format        VARCHAR(10)  NOT NULL,
    provider_code VARCHAR(60)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE report_job (
    id              BIGSERIAL PRIMARY KEY,
    definition_code VARCHAR(60)   NOT NULL,
    parameters      VARCHAR(2000),
    status          VARCHAR(20)   NOT NULL,
    requested_by    VARCHAR(100)  NOT NULL,
    -- A report is always run for one tenant; it must never span them.
    tenant_id       VARCHAR(63)   NOT NULL,
    storage_key     VARCHAR(300),
    size_bytes      BIGINT,
    row_count       INT,
    error           VARCHAR(1000),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ
);

CREATE INDEX idx_report_job_requested_by ON report_job(requested_by);
CREATE INDEX idx_report_job_status ON report_job(status);

INSERT INTO report_definition (code, name, description, format, provider_code) VALUES
    ('TENDER_SUMMARY_XLSX', 'Tender Summary (Excel)',
     'Status and value of tenders', 'XLSX', 'TENDER_SUMMARY'),
    ('TENDER_SUMMARY_CSV', 'Tender Summary (CSV)',
     'Status and value of tenders, for import elsewhere', 'CSV', 'TENDER_SUMMARY'),
    ('SUPPLIER_REGISTER_XLSX', 'Supplier Register (Excel)',
     'Registered suppliers and their eligibility', 'XLSX', 'SUPPLIER_REGISTER');
