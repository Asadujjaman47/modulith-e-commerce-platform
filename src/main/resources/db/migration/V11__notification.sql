-- V11__notification.sql
-- Notification module schema. The module is event-driven and has no public API.
--
-- notification_recipients is a local replica of the contact details the module needs to address
-- outbound messages. It is populated from UserRegisteredEvent (auth user id + email) so the module
-- never depends on the user module: every later event (order/payment/shipment) carries the same
-- customer_id == auth user_id, which is resolved against this table. References auth/customer ids by
-- value only (no cross-module FKs), per the module boundary rules.
--
-- notification_logs is an append-only record of every delivery attempt (SENT or FAILED). reference_id
-- links a notification back to the business entity that triggered it (order/payment/shipment id) by
-- value, for traceability.

CREATE TABLE notification_recipients (
    id         UUID         NOT NULL,
    user_id    UUID         NOT NULL,
    email      VARCHAR(320) NOT NULL,
    first_name VARCHAR(255),
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT pk_notification_recipients PRIMARY KEY (id),
    CONSTRAINT uq_notification_recipients_user UNIQUE (user_id)
);

CREATE TABLE notification_logs (
    id             UUID         NOT NULL,
    type           VARCHAR(40)  NOT NULL,
    channel        VARCHAR(20)  NOT NULL,
    recipient      VARCHAR(320) NOT NULL,
    subject        VARCHAR(500) NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    reference_id   UUID,
    failure_reason VARCHAR(500),
    sent_at        TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    created_by     VARCHAR(255),
    updated_by     VARCHAR(255),
    CONSTRAINT pk_notification_logs PRIMARY KEY (id)
);

CREATE INDEX idx_notification_logs_recipient ON notification_logs (recipient);
CREATE INDEX idx_notification_logs_type ON notification_logs (type);
CREATE INDEX idx_notification_logs_reference_id ON notification_logs (reference_id);
