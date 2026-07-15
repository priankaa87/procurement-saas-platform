-- Evaluation schema: criteria, participants, technical scores, and derived results.
-- Applied per provisioned tenant schema (schema-per-tenant).
-- tender_code and supplier_code reference the Tender and Vendor services by business
-- code — no cross-database foreign keys.

CREATE TABLE evaluation (
    id               BIGSERIAL PRIMARY KEY,
    tender_code      VARCHAR(50)  NOT NULL UNIQUE,
    status           VARCHAR(20)  NOT NULL,
    technical_weight INT          NOT NULL,
    financial_weight INT          NOT NULL,
    pass_mark        NUMERIC(5,2) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_evaluation_weights CHECK (technical_weight + financial_weight = 100)
);

CREATE TABLE evaluation_criterion (
    id            BIGSERIAL PRIMARY KEY,
    evaluation_id BIGINT       NOT NULL REFERENCES evaluation(id) ON DELETE CASCADE,
    code          VARCHAR(50)  NOT NULL,
    name          VARCHAR(200) NOT NULL,
    weight        INT          NOT NULL,
    CONSTRAINT uq_criterion_evaluation_code UNIQUE (evaluation_id, code),
    CONSTRAINT ck_criterion_weight CHECK (weight > 0 AND weight <= 100)
);

CREATE TABLE participant_evaluation (
    id              BIGSERIAL PRIMARY KEY,
    evaluation_id   BIGINT        NOT NULL REFERENCES evaluation(id) ON DELETE CASCADE,
    supplier_code   VARCHAR(50)   NOT NULL,
    bid_amount      NUMERIC(18,2) NOT NULL,
    technical_score NUMERIC(6,2),
    financial_score NUMERIC(6,2),
    combined_score  NUMERIC(6,2),
    qualified       BOOLEAN,
    final_rank      INT,
    CONSTRAINT uq_participant_evaluation UNIQUE (evaluation_id, supplier_code),
    CONSTRAINT ck_bid_amount_positive CHECK (bid_amount > 0)
);

CREATE TABLE criterion_score (
    id                       BIGSERIAL PRIMARY KEY,
    participant_evaluation_id BIGINT       NOT NULL REFERENCES participant_evaluation(id) ON DELETE CASCADE,
    criterion_id             BIGINT       NOT NULL REFERENCES evaluation_criterion(id) ON DELETE CASCADE,
    score                    NUMERIC(6,2) NOT NULL,
    comment                  VARCHAR(500),
    CONSTRAINT uq_score_participant_criterion UNIQUE (participant_evaluation_id, criterion_id),
    CONSTRAINT ck_score_range CHECK (score >= 0 AND score <= 100)
);

CREATE INDEX idx_criterion_evaluation ON evaluation_criterion(evaluation_id);
CREATE INDEX idx_participant_evaluation ON participant_evaluation(evaluation_id);
CREATE INDEX idx_score_participant ON criterion_score(participant_evaluation_id);
