-- Tender schema: tenders, line items, invited participants, sealed bids.
-- Applied per provisioned tenant schema (schema-per-tenant).
-- item_code / unit_code / currency_code are Master Data codes; supplier_code is owned by
-- Vendor Management — referenced by business code, not by cross-database foreign key.

CREATE TABLE tender (
    id                    BIGSERIAL PRIMARY KEY,
    code                  VARCHAR(50)  NOT NULL UNIQUE,
    title                 VARCHAR(250) NOT NULL,
    description           VARCHAR(2000),
    status                VARCHAR(20)  NOT NULL,
    currency_code         VARCHAR(3)   NOT NULL,
    bid_deadline          TIMESTAMPTZ  NOT NULL,
    published_at          TIMESTAMPTZ,
    opened_at             TIMESTAMPTZ,
    awarded_supplier_code VARCHAR(50),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE tender_item (
    id          BIGSERIAL PRIMARY KEY,
    tender_id   BIGINT         NOT NULL REFERENCES tender(id) ON DELETE CASCADE,
    item_code   VARCHAR(50)    NOT NULL,
    description VARCHAR(500),
    quantity    NUMERIC(18,3)  NOT NULL,
    unit_code   VARCHAR(30)    NOT NULL
);

CREATE TABLE tender_participant (
    id            BIGSERIAL PRIMARY KEY,
    tender_id     BIGINT      NOT NULL REFERENCES tender(id) ON DELETE CASCADE,
    supplier_code VARCHAR(50) NOT NULL,
    status        VARCHAR(20) NOT NULL,
    invited_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_participant_tender_supplier UNIQUE (tender_id, supplier_code)
);

-- One bid per supplier per tender, enforced by the database rather than by hope.
CREATE TABLE bid (
    id            BIGSERIAL PRIMARY KEY,
    tender_id     BIGINT         NOT NULL REFERENCES tender(id) ON DELETE CASCADE,
    supplier_code VARCHAR(50)    NOT NULL,
    total_amount  NUMERIC(18,2)  NOT NULL,
    currency_code VARCHAR(3)     NOT NULL,
    notes         VARCHAR(1000),
    submitted_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_bid_tender_supplier UNIQUE (tender_id, supplier_code)
);

CREATE INDEX idx_tender_status ON tender(status);
CREATE INDEX idx_tender_item_tender ON tender_item(tender_id);
CREATE INDEX idx_tender_participant_tender ON tender_participant(tender_id);
CREATE INDEX idx_bid_tender ON bid(tender_id);
