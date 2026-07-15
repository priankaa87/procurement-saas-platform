-- Control-plane schema: the tenant registry, plans, usage ledger, and invoices.
-- This lives in the shared schema, NOT in a tenant schema — it is the thing that creates
-- tenant schemas.

CREATE TABLE plan (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(40)   NOT NULL UNIQUE,
    name          VARCHAR(100)  NOT NULL,
    price_monthly NUMERIC(12,2) NOT NULL,
    currency_code VARCHAR(3)    NOT NULL,
    -- -1 means unlimited
    max_users     INT           NOT NULL,
    max_tenders   INT           NOT NULL
);

CREATE TABLE tenant (
    id            BIGSERIAL PRIMARY KEY,
    tenant_key    VARCHAR(30)  NOT NULL UNIQUE,
    name          VARCHAR(200) NOT NULL,
    schema_name   VARCHAR(40)  NOT NULL UNIQUE,
    status        VARCHAR(20)  NOT NULL,
    plan_code     VARCHAR(40)  NOT NULL,
    contact_email VARCHAR(150),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    activated_at  TIMESTAMPTZ
);

CREATE TABLE usage_record (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT      NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    metric      VARCHAR(40) NOT NULL,
    quantity    BIGINT      NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE invoice (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT        NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    plan_code     VARCHAR(40)   NOT NULL,
    period_start  DATE          NOT NULL,
    period_end    DATE          NOT NULL,
    amount        NUMERIC(12,2) NOT NULL,
    currency_code VARCHAR(3)    NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    issued_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    paid_at       TIMESTAMPTZ,
    -- One invoice per tenant per period; re-running billing must not double-charge.
    CONSTRAINT uq_invoice_tenant_period UNIQUE (tenant_id, period_start)
);

CREATE INDEX idx_tenant_status ON tenant(status);
CREATE INDEX idx_usage_tenant_metric ON usage_record(tenant_id, metric);
CREATE INDEX idx_invoice_tenant ON invoice(tenant_id);

INSERT INTO plan (code, name, price_monthly, currency_code, max_users, max_tenders) VALUES
    ('FREE',       'Free Trial',  0.00,   'USD',  3,  2),
    ('STARTER',    'Starter',     99.00,  'USD', 10, 25),
    ('PROFESSIONAL','Professional',499.00,'USD', 50, -1),
    ('ENTERPRISE', 'Enterprise',  1999.00,'USD', -1, -1);
