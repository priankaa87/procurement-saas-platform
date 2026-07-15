-- Vendor management schema: suppliers, contacts, documents, debarment.
-- Applied per provisioned tenant schema (schema-per-tenant).
-- Note: country_iso2 and category_code reference Master Data by business code, not by
-- foreign key — each service owns its own schema.

CREATE TABLE supplier (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(50)  NOT NULL UNIQUE,
    name         VARCHAR(200) NOT NULL,
    legal_name   VARCHAR(250),
    email        VARCHAR(150),
    phone        VARCHAR(50),
    tax_id       VARCHAR(50),
    status       VARCHAR(20)  NOT NULL,
    country_iso2 VARCHAR(2),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE supplier_category (
    supplier_id   BIGINT      NOT NULL REFERENCES supplier(id) ON DELETE CASCADE,
    category_code VARCHAR(50) NOT NULL,
    PRIMARY KEY (supplier_id, category_code)
);

CREATE TABLE supplier_contact (
    id              BIGSERIAL PRIMARY KEY,
    supplier_id     BIGINT       NOT NULL REFERENCES supplier(id) ON DELETE CASCADE,
    name            VARCHAR(150) NOT NULL,
    email           VARCHAR(150),
    phone           VARCHAR(50),
    primary_contact BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE supplier_document (
    id          BIGSERIAL PRIMARY KEY,
    supplier_id BIGINT       NOT NULL REFERENCES supplier(id) ON DELETE CASCADE,
    doc_type    VARCHAR(50)  NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    uploaded_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at  DATE
);

CREATE TABLE supplier_debarment (
    id             BIGSERIAL PRIMARY KEY,
    supplier_id    BIGINT       NOT NULL REFERENCES supplier(id) ON DELETE CASCADE,
    reason         VARCHAR(500) NOT NULL,
    debarred_from  DATE         NOT NULL,
    debarred_until DATE,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_supplier_status ON supplier(status);
CREATE INDEX idx_supplier_contact_supplier ON supplier_contact(supplier_id);
CREATE INDEX idx_supplier_document_supplier ON supplier_document(supplier_id);
CREATE INDEX idx_supplier_debarment_supplier ON supplier_debarment(supplier_id);
