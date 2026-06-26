-- V13__reporting.sql
-- Reporting module schema: read-only sales projections built from order events.
--
-- The reporting module owns no business aggregates. It records immutable per-order facts whenever an
-- OrderCreatedEvent is observed and computes sales/product reports by aggregating these facts at query
-- time. order_id / customer_id / product_id reference other modules by value only (no cross-module
-- FKs), per the module boundary rules.
--
-- sales_facts holds one row per placed order (order_id is unique, so replayed events are ignored).
-- product_sales_facts holds one row per order line, enabling per-product unit reporting.

CREATE TABLE sales_facts (
    id             UUID          NOT NULL,
    order_id       UUID          NOT NULL,
    customer_id    UUID          NOT NULL,
    order_date     DATE          NOT NULL,
    order_total    NUMERIC(19, 2) NOT NULL,
    discount_total NUMERIC(19, 2) NOT NULL,
    item_count     INTEGER       NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL,
    created_by     VARCHAR(255),
    updated_by     VARCHAR(255),
    CONSTRAINT pk_sales_facts PRIMARY KEY (id),
    CONSTRAINT uq_sales_facts_order UNIQUE (order_id)
);

CREATE INDEX idx_sales_facts_order_date ON sales_facts (order_date);

CREATE TABLE product_sales_facts (
    id          UUID        NOT NULL,
    order_id    UUID        NOT NULL,
    product_id  UUID        NOT NULL,
    quantity    INTEGER     NOT NULL,
    order_date  DATE        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    CONSTRAINT pk_product_sales_facts PRIMARY KEY (id),
    CONSTRAINT uq_product_sales_facts_order_product UNIQUE (order_id, product_id)
);

CREATE INDEX idx_product_sales_facts_product_id ON product_sales_facts (product_id);
CREATE INDEX idx_product_sales_facts_order_date ON product_sales_facts (order_date);
