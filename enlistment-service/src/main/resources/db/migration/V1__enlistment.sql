-- Enlistment schema: pre-qualification rounds, applications, assessment, and the
-- resulting time-bounded enlistments.
-- Applied per provisioned tenant schema (schema-per-tenant).
-- supplier_code / category_code reference Vendor and Master Data by business code.

CREATE TABLE enlistment_schedule (
    id                   BIGSERIAL PRIMARY KEY,
    code                 VARCHAR(50)  NOT NULL UNIQUE,
    title                VARCHAR(250) NOT NULL,
    description          VARCHAR(2000),
    category_code        VARCHAR(50)  NOT NULL,
    status               VARCHAR(20)  NOT NULL,
    application_deadline TIMESTAMPTZ  NOT NULL,
    pass_mark            NUMERIC(5,2) NOT NULL,
    validity_months      INT          NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at         TIMESTAMPTZ,
    CONSTRAINT ck_schedule_validity CHECK (validity_months >= 1),
    CONSTRAINT ck_schedule_pass_mark CHECK (pass_mark >= 0 AND pass_mark <= 100)
);

CREATE TABLE enlistment_criterion (
    id          BIGSERIAL PRIMARY KEY,
    schedule_id BIGINT       NOT NULL REFERENCES enlistment_schedule(id) ON DELETE CASCADE,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(200) NOT NULL,
    weight      INT          NOT NULL,
    -- A gate rather than a weight: failing a mandatory criterion disqualifies outright.
    mandatory   BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_criterion_schedule_code UNIQUE (schedule_id, code),
    CONSTRAINT ck_criterion_weight CHECK (weight > 0 AND weight <= 100)
);

CREATE TABLE enlistment_application (
    id              BIGSERIAL PRIMARY KEY,
    schedule_id     BIGINT       NOT NULL REFERENCES enlistment_schedule(id) ON DELETE CASCADE,
    supplier_code   VARCHAR(50)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    score           NUMERIC(6,2),
    decision_reason VARCHAR(500),
    submitted_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    decided_at      TIMESTAMPTZ,
    -- One application per supplier per round.
    CONSTRAINT uq_application_schedule_supplier UNIQUE (schedule_id, supplier_code)
);

CREATE TABLE criterion_assessment (
    id             BIGSERIAL PRIMARY KEY,
    application_id BIGINT       NOT NULL REFERENCES enlistment_application(id) ON DELETE CASCADE,
    criterion_id   BIGINT       NOT NULL REFERENCES enlistment_criterion(id) ON DELETE CASCADE,
    score          NUMERIC(6,2) NOT NULL,
    met            BOOLEAN      NOT NULL,
    comment        VARCHAR(500),
    CONSTRAINT uq_assessment_application_criterion UNIQUE (application_id, criterion_id),
    CONSTRAINT ck_assessment_score CHECK (score >= 0 AND score <= 100)
);

CREATE TABLE enlistment (
    id             BIGSERIAL PRIMARY KEY,
    supplier_code  VARCHAR(50) NOT NULL,
    category_code  VARCHAR(50) NOT NULL,
    schedule_code  VARCHAR(50) NOT NULL,
    valid_from     DATE        NOT NULL,
    valid_until    DATE        NOT NULL,
    status         VARCHAR(20) NOT NULL,
    revoked_reason VARCHAR(500),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_enlistment_window CHECK (valid_until > valid_from)
);

CREATE INDEX idx_schedule_status ON enlistment_schedule(status);
CREATE INDEX idx_criterion_schedule ON enlistment_criterion(schedule_id);
CREATE INDEX idx_application_schedule ON enlistment_application(schedule_id);
CREATE INDEX idx_assessment_application ON criterion_assessment(application_id);
CREATE INDEX idx_enlistment_supplier ON enlistment(supplier_code);
CREATE INDEX idx_enlistment_category ON enlistment(category_code);
