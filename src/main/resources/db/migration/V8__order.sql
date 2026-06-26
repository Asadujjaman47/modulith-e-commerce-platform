-- V8__order.sql
-- Order module schema: customer orders, their line items, and a snapshotted shipping address.
--
-- customer_id, product_id and coupon_code reference the user/catalog/coupon modules by value only
-- (no cross-module FKs), per the module boundary rules. Each order item snapshots the product name
-- and unit price at placement time, and order_addresses snapshots the chosen shipping address, so an
-- order remains stable even if the catalog price or the customer's address later changes.
--
-- idempotency_key (optional, supplied via the Idempotency-Key header) is unique per customer so a
-- retried place-order request returns the original order instead of creating a duplicate.

CREATE TABLE orders (
    id              UUID           NOT NULL,
    order_number    VARCHAR(32)    NOT NULL,
    customer_id     UUID           NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    currency        VARCHAR(3)     NOT NULL,
    subtotal        NUMERIC(12, 2) NOT NULL,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_amount    NUMERIC(12, 2) NOT NULL,
    coupon_code     VARCHAR(64),
    idempotency_key VARCHAR(255),
    placed_at       TIMESTAMPTZ    NOT NULL,
    cancelled_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT uq_orders_order_number UNIQUE (order_number),
    CONSTRAINT uq_orders_customer_idempotency UNIQUE (customer_id, idempotency_key),
    CONSTRAINT ck_orders_subtotal CHECK (subtotal >= 0),
    CONSTRAINT ck_orders_discount CHECK (discount_amount >= 0),
    CONSTRAINT ck_orders_total CHECK (total_amount >= 0)
);

CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_orders_status ON orders (status);

CREATE TABLE order_items (
    id           UUID           NOT NULL,
    order_id     UUID           NOT NULL,
    product_id   UUID           NOT NULL,
    product_name VARCHAR(200)   NOT NULL,
    unit_price   NUMERIC(12, 2) NOT NULL,
    quantity     INTEGER        NOT NULL,
    created_at   TIMESTAMPTZ    NOT NULL,
    updated_at   TIMESTAMPTZ    NOT NULL,
    created_by   VARCHAR(255),
    updated_by   VARCHAR(255),
    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);

CREATE TABLE order_addresses (
    id          UUID         NOT NULL,
    order_id    UUID         NOT NULL,
    label       VARCHAR(255),
    line1       VARCHAR(255) NOT NULL,
    line2       VARCHAR(255),
    city        VARCHAR(255) NOT NULL,
    state       VARCHAR(255),
    postal_code VARCHAR(64)  NOT NULL,
    country     VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    CONSTRAINT pk_order_addresses PRIMARY KEY (id),
    CONSTRAINT fk_order_addresses_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT uq_order_addresses_order UNIQUE (order_id)
);

CREATE INDEX idx_order_addresses_order_id ON order_addresses (order_id);
