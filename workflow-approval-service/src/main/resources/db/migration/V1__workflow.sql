-- Workflow & approval schema: templates, steps, requests, actions, delegations.
-- Applied per provisioned tenant schema (schema-per-tenant).
-- subject_ref points at a tender/award/supplier by business code — this engine holds no
-- foreign key to the thing being approved and does not know what it is.

CREATE TABLE approval_workflow (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(50)  NOT NULL UNIQUE,
    name         VARCHAR(150) NOT NULL,
    subject_type VARCHAR(40)  NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE approval_step (
    id          BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT       NOT NULL REFERENCES approval_workflow(id) ON DELETE CASCADE,
    step_no     INT          NOT NULL,
    name        VARCHAR(150) NOT NULL,
    role_code   VARCHAR(60)  NOT NULL,
    CONSTRAINT uq_step_workflow_no UNIQUE (workflow_id, step_no),
    CONSTRAINT ck_step_no_positive CHECK (step_no >= 1)
);

CREATE TABLE approval_request (
    id            BIGSERIAL PRIMARY KEY,
    workflow_code VARCHAR(50) NOT NULL,
    subject_type  VARCHAR(40) NOT NULL,
    subject_ref   VARCHAR(80) NOT NULL,
    requested_by  VARCHAR(100) NOT NULL,
    reason        VARCHAR(500),
    status        VARCHAR(20) NOT NULL,
    current_step  INT         NOT NULL,
    total_steps   INT         NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at  TIMESTAMPTZ
);

CREATE TABLE approval_action (
    id           BIGSERIAL PRIMARY KEY,
    request_id   BIGINT       NOT NULL REFERENCES approval_request(id) ON DELETE CASCADE,
    step_no      INT          NOT NULL,
    role_code    VARCHAR(60)  NOT NULL,
    -- Who actually acted.
    actor_id     VARCHAR(100) NOT NULL,
    -- Whose authority they used, if acting under a delegation.
    on_behalf_of VARCHAR(100),
    decision     VARCHAR(20)  NOT NULL,
    comment      VARCHAR(500),
    acted_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE delegation (
    id         BIGSERIAL PRIMARY KEY,
    from_user  VARCHAR(100) NOT NULL,
    to_user    VARCHAR(100) NOT NULL,
    role_code  VARCHAR(60)  NOT NULL,
    valid_from DATE         NOT NULL,
    valid_to   DATE         NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    reason     VARCHAR(500),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_delegation_not_self CHECK (from_user <> to_user),
    CONSTRAINT ck_delegation_window CHECK (valid_to >= valid_from)
);

-- Only one live request per subject: asking twice while one is open invites two answers.
CREATE UNIQUE INDEX uq_open_request_per_subject
    ON approval_request (subject_type, subject_ref)
    WHERE status = 'PENDING';

CREATE INDEX idx_step_workflow ON approval_step(workflow_id);
CREATE INDEX idx_request_status ON approval_request(status);
CREATE INDEX idx_action_request ON approval_action(request_id);
CREATE INDEX idx_delegation_to_role ON delegation(to_user, role_code);

-- A representative workflow: publishing a tender needs procurement, then finance.
INSERT INTO approval_workflow (code, name, subject_type) VALUES
    ('TENDER_PUBLISH', 'Tender publication approval', 'TENDER'),
    ('SUPPLIER_DEBAR', 'Supplier debarment approval', 'SUPPLIER');

INSERT INTO approval_step (workflow_id, step_no, name, role_code)
SELECT id, 1, 'Procurement review', 'PROCUREMENT_OFFICER' FROM approval_workflow WHERE code = 'TENDER_PUBLISH';
INSERT INTO approval_step (workflow_id, step_no, name, role_code)
SELECT id, 2, 'Finance approval', 'FINANCE_MANAGER' FROM approval_workflow WHERE code = 'TENDER_PUBLISH';

INSERT INTO approval_step (workflow_id, step_no, name, role_code)
SELECT id, 1, 'Compliance review', 'COMPLIANCE_OFFICER' FROM approval_workflow WHERE code = 'SUPPLIER_DEBAR';
