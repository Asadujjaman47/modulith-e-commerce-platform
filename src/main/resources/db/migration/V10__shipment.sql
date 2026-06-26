-- V10__shipment.sql
-- Shipment module schema: one shipment per order plus an append-only tracking history.
--
-- order_id and customer_id reference the order/user modules by value only (no cross-module FKs), per
-- the module boundary rules. A shipment is created when an order is paid, snapshotting the delivery
-- address (the address_* columns) so the destination is stable even if the order/customer address
-- later changes. order_id and tracking_number are unique.

CREATE TABLE shipments (
    id                  UUID         NOT NULL,
    order_id            UUID         NOT NULL,
    customer_id         UUID         NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    carrier             VARCHAR(100) NOT NULL,
    tracking_number     VARCHAR(64)  NOT NULL,
    address_label       VARCHAR(255),
    address_line1       VARCHAR(255) NOT NULL,
    address_line2       VARCHAR(255),
    address_city        VARCHAR(255) NOT NULL,
    address_state       VARCHAR(255),
    address_postal_code VARCHAR(64)  NOT NULL,
    address_country     VARCHAR(255) NOT NULL,
    shipped_at          TIMESTAMPTZ,
    delivered_at        TIMESTAMPTZ,
    estimated_delivery  TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    CONSTRAINT pk_shipments PRIMARY KEY (id),
    CONSTRAINT uq_shipments_order UNIQUE (order_id),
    CONSTRAINT uq_shipments_tracking_number UNIQUE (tracking_number)
);

CREATE INDEX idx_shipments_customer_id ON shipments (customer_id);
CREATE INDEX idx_shipments_status ON shipments (status);

CREATE TABLE tracking_records (
    id          UUID         NOT NULL,
    shipment_id UUID         NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    location    VARCHAR(255),
    note        VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    CONSTRAINT pk_tracking_records PRIMARY KEY (id),
    CONSTRAINT fk_tracking_records_shipment FOREIGN KEY (shipment_id) REFERENCES shipments (id)
);

CREATE INDEX idx_tracking_records_shipment_id ON tracking_records (shipment_id);
