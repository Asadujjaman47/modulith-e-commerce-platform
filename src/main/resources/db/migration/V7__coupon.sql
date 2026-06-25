-- V7__coupon.sql
-- Coupon module schema: discount coupons and per-customer usage records.
--
-- coupon_usages references coupons by value (coupon_id) and the customer by value (customer_id);
-- order_id is nullable until the order module links usages to concrete orders. No cross-module FKs.

CREATE TABLE coupons (
    id                  UUID           NOT NULL,
    code                VARCHAR(64)    NOT NULL,
    description         VARCHAR(255),
    discount_type       VARCHAR(20)    NOT NULL,
    discount_value      NUMERIC(12, 2) NOT NULL,
    min_order_amount    NUMERIC(12, 2),
    max_discount_amount NUMERIC(12, 2),
    valid_from          TIMESTAMPTZ    NOT NULL,
    valid_until         TIMESTAMPTZ    NOT NULL,
    usage_limit         INTEGER,
    times_used          INTEGER        NOT NULL DEFAULT 0,
    active              BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ    NOT NULL,
    updated_at          TIMESTAMPTZ    NOT NULL,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    CONSTRAINT pk_coupons PRIMARY KEY (id),
    CONSTRAINT uq_coupons_code UNIQUE (code),
    CONSTRAINT ck_coupons_discount_value CHECK (discount_value > 0),
    CONSTRAINT ck_coupons_times_used CHECK (times_used >= 0),
    CONSTRAINT ck_coupons_validity CHECK (valid_until >= valid_from)
);

CREATE INDEX idx_coupons_active ON coupons (active);

CREATE TABLE coupon_usages (
    id              UUID           NOT NULL,
    coupon_id       UUID           NOT NULL,
    customer_id     UUID           NOT NULL,
    order_id        UUID,
    discount_amount NUMERIC(12, 2) NOT NULL,
    used_at         TIMESTAMPTZ    NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    CONSTRAINT pk_coupon_usages PRIMARY KEY (id)
);

CREATE INDEX idx_coupon_usages_coupon_id ON coupon_usages (coupon_id);
CREATE INDEX idx_coupon_usages_customer_id ON coupon_usages (customer_id);
