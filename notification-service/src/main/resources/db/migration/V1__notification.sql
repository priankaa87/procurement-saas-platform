-- Notification schema: templates and rendered messages with delivery status.
-- Applied per provisioned tenant schema (schema-per-tenant).

CREATE TABLE notification_template (
    id      BIGSERIAL PRIMARY KEY,
    code    VARCHAR(60)   NOT NULL UNIQUE,
    subject VARCHAR(250)  NOT NULL,
    body    VARCHAR(4000) NOT NULL
);

CREATE TABLE notification (
    id            BIGSERIAL PRIMARY KEY,
    recipient     VARCHAR(100)  NOT NULL,
    template_code VARCHAR(60)   NOT NULL,
    subject       VARCHAR(250)  NOT NULL,
    body          VARCHAR(4000) NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    -- Idempotency: Kafka will redeliver, and nobody should be emailed twice for it.
    event_key     VARCHAR(200)  NOT NULL UNIQUE,
    error         VARCHAR(500),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    sent_at       TIMESTAMPTZ
);

CREATE INDEX idx_notification_recipient ON notification(recipient);
CREATE INDEX idx_notification_status ON notification(status);

INSERT INTO notification_template (code, subject, body) VALUES
    ('TENDER_PUBLISHED',
     'Invitation to bid: {{tenderTitle}}',
     'You have been invited to bid on tender {{tenderCode}} ({{tenderTitle}}). '
     || 'Bids close at {{bidDeadline}}. Please submit before the deadline.'),
    ('TENDER_AWARDED_WINNER',
     'You have been awarded: {{tenderTitle}}',
     'Congratulations. Tender {{tenderCode}} ({{tenderTitle}}) has been awarded to you. '
     || 'Our team will contact you regarding the work order.'),
    ('TENDER_AWARDED_UNSUCCESSFUL',
     'Outcome of tender {{tenderCode}}',
     'Thank you for bidding on {{tenderCode}} ({{tenderTitle}}). '
     || 'On this occasion the tender was awarded to another supplier.'),
    ('SUPPLIER_DEBARRED',
     'Notice of debarment',
     'Supplier {{supplierCode}} has been debarred. Reason: {{reason}}.');
