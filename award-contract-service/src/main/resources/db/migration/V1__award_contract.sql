-- Award & contract schema: notice of award, work orders, delivery lines, goods receipts.
-- Applied per provisioned tenant schema (schema-per-tenant).
-- tender_code / supplier_code / item_code reference the Tender, Vendor and Master Data
-- services by business code — no cross-database foreign keys.

CREATE TABLE award (
    id             BIGSERIAL PRIMARY KEY,
    tender_code    VARCHAR(50)   NOT NULL UNIQUE,
    tender_title   VARCHAR(250),
    supplier_code  VARCHAR(50)   NOT NULL,
    amount         NUMERIC(18,2) NOT NULL,
    currency_code  VARCHAR(3)    NOT NULL,
    status         VARCHAR(25)   NOT NULL,
    issued_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    respond_by     DATE          NOT NULL,
    responded_at   TIMESTAMPTZ,
    decline_reason VARCHAR(500),
    CONSTRAINT ck_award_amount_positive CHECK (amount > 0)
);

CREATE TABLE work_order (
    id            BIGSERIAL PRIMARY KEY,
    award_id      BIGINT        NOT NULL REFERENCES award(id) ON DELETE CASCADE,
    code          VARCHAR(50)   NOT NULL UNIQUE,
    status        VARCHAR(20)   NOT NULL,
    total_amount  NUMERIC(18,2) NOT NULL,
    currency_code VARCHAR(3)    NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    issued_at     TIMESTAMPTZ,
    completed_at  TIMESTAMPTZ
);

CREATE TABLE delivery_line (
    id                BIGSERIAL PRIMARY KEY,
    work_order_id     BIGINT         NOT NULL REFERENCES work_order(id) ON DELETE CASCADE,
    line_no           INT            NOT NULL,
    item_code         VARCHAR(50)    NOT NULL,
    ordered_quantity  NUMERIC(18,3)  NOT NULL,
    received_quantity NUMERIC(18,3)  NOT NULL DEFAULT 0,
    unit_code         VARCHAR(30)    NOT NULL,
    due_date          DATE           NOT NULL,
    status            VARCHAR(20)    NOT NULL,
    CONSTRAINT uq_delivery_line UNIQUE (work_order_id, line_no),
    CONSTRAINT ck_ordered_positive CHECK (ordered_quantity > 0),
    -- The database enforces it too: nobody may be paid for more than was ordered.
    CONSTRAINT ck_not_over_received CHECK (received_quantity <= ordered_quantity)
);

CREATE TABLE goods_receipt (
    id               BIGSERIAL PRIMARY KEY,
    delivery_line_id BIGINT        NOT NULL REFERENCES delivery_line(id) ON DELETE CASCADE,
    quantity         NUMERIC(18,3) NOT NULL,
    received_by      VARCHAR(100)  NOT NULL,
    received_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    late             BOOLEAN       NOT NULL DEFAULT FALSE,
    remarks          VARCHAR(500),
    CONSTRAINT ck_receipt_positive CHECK (quantity > 0)
);

CREATE INDEX idx_award_status ON award(status);
CREATE INDEX idx_work_order_award ON work_order(award_id);
CREATE INDEX idx_delivery_line_work_order ON delivery_line(work_order_id);
CREATE INDEX idx_goods_receipt_line ON goods_receipt(delivery_line_id);
