-- V9__payment.sql
-- Payment module schema: one payment per order plus an append-only ledger of gateway interactions.
--
-- order_id and customer_id reference the order/user modules by value only (no cross-module FKs), per
-- the module boundary rules. A payment is created as a PENDING intent when the order is placed and
-- driven to SUCCESS/FAILED by a gateway charge, then optionally REFUNDED. order_id is unique so there
-- is exactly one payment per order; idempotency_key (optional, supplied via the Idempotency-Key
-- header) is unique per customer so a retried process-payment request is safe.

CREATE TABLE payments (
    id                UUID           NOT NULL,
    order_id          UUID           NOT NULL,
    customer_id       UUID           NOT NULL,
    status            VARCHAR(20)    NOT NULL,
    method            VARCHAR(20),
    amount            NUMERIC(12, 2) NOT NULL,
    currency          VARCHAR(3)     NOT NULL,
    idempotency_key   VARCHAR(255),
    gateway_reference VARCHAR(255),
    failure_reason    VARCHAR(500),
    paid_at           TIMESTAMPTZ,
    failed_at         TIMESTAMPTZ,
    refunded_at       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ    NOT NULL,
    updated_at        TIMESTAMPTZ    NOT NULL,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255),
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uq_payments_order UNIQUE (order_id),
    CONSTRAINT uq_payments_customer_idempotency UNIQUE (customer_id, idempotency_key),
    CONSTRAINT ck_payments_amount CHECK (amount > 0)
);

CREATE INDEX idx_payments_customer_id ON payments (customer_id);
CREATE INDEX idx_payments_status ON payments (status);

CREATE TABLE payment_transactions (
    id                UUID           NOT NULL,
    payment_id        UUID           NOT NULL,
    type              VARCHAR(20)    NOT NULL,
    succeeded         BOOLEAN        NOT NULL,
    amount            NUMERIC(12, 2) NOT NULL,
    gateway_reference VARCHAR(255),
    message           VARCHAR(500),
    created_at        TIMESTAMPTZ    NOT NULL,
    updated_at        TIMESTAMPTZ    NOT NULL,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255),
    CONSTRAINT pk_payment_transactions PRIMARY KEY (id),
    CONSTRAINT fk_payment_transactions_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
);

CREATE INDEX idx_payment_transactions_payment_id ON payment_transactions (payment_id);
