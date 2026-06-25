-- V6__cart.sql
-- Cart module schema: one cart per customer with its line items.
--
-- customer_id and product_id reference the user/catalog modules by value only (no cross-module
-- FKs), per the module boundary rules. Each cart item snapshots the product name and unit price at
-- the time it was added; the snapshot is refreshed when the catalog publishes ProductUpdatedEvent.

CREATE TABLE carts (
    id          UUID        NOT NULL,
    customer_id UUID        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    CONSTRAINT pk_carts PRIMARY KEY (id),
    CONSTRAINT uq_carts_customer_id UNIQUE (customer_id)
);

CREATE TABLE cart_items (
    id           UUID           NOT NULL,
    cart_id      UUID           NOT NULL,
    product_id   UUID           NOT NULL,
    product_name VARCHAR(200)   NOT NULL,
    unit_price   NUMERIC(12, 2) NOT NULL,
    quantity     INTEGER        NOT NULL,
    created_at   TIMESTAMPTZ    NOT NULL,
    updated_at   TIMESTAMPTZ    NOT NULL,
    created_by   VARCHAR(255),
    updated_by   VARCHAR(255),
    CONSTRAINT pk_cart_items PRIMARY KEY (id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id),
    CONSTRAINT uq_cart_items_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT ck_cart_items_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_cart_items_cart_id ON cart_items (cart_id);
CREATE INDEX idx_cart_items_product_id ON cart_items (product_id);
