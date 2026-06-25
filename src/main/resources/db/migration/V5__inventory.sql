-- V5__inventory.sql
-- Inventory module schema: per-product stock, reservations and an append-only transaction ledger.
--
-- product_id references the catalog product by value only (no cross-module FK), per the module
-- boundary rules. Inventory rows are seeded by the inventory module when it consumes the catalog
-- ProductCreatedEvent.

CREATE TABLE inventory (
    id                UUID        NOT NULL,
    product_id        UUID        NOT NULL,
    quantity_on_hand  INTEGER     NOT NULL DEFAULT 0,
    quantity_reserved INTEGER     NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255),
    CONSTRAINT pk_inventory PRIMARY KEY (id),
    CONSTRAINT uq_inventory_product_id UNIQUE (product_id),
    CONSTRAINT ck_inventory_non_negative CHECK (quantity_on_hand >= 0 AND quantity_reserved >= 0),
    CONSTRAINT ck_inventory_reserved_within_on_hand CHECK (quantity_reserved <= quantity_on_hand)
);

CREATE TABLE stock_reservations (
    id         UUID        NOT NULL,
    product_id UUID        NOT NULL,
    quantity   INTEGER     NOT NULL,
    status     VARCHAR(20) NOT NULL,
    reference  VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT pk_stock_reservations PRIMARY KEY (id),
    CONSTRAINT ck_stock_reservations_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_stock_reservations_product_id ON stock_reservations (product_id);
CREATE INDEX idx_stock_reservations_status ON stock_reservations (status);

CREATE TABLE inventory_transactions (
    id             UUID        NOT NULL,
    product_id     UUID        NOT NULL,
    type           VARCHAR(20) NOT NULL,
    quantity_delta INTEGER     NOT NULL,
    reason         VARCHAR(255),
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    created_by     VARCHAR(255),
    updated_by     VARCHAR(255),
    CONSTRAINT pk_inventory_transactions PRIMARY KEY (id)
);

CREATE INDEX idx_inventory_transactions_product_id ON inventory_transactions (product_id);