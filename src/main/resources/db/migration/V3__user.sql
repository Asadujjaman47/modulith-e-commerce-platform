-- V3__user.sql
-- User module schema: customer profiles and addresses.
--
-- customers.user_id references the auth user id by value only (no cross-module FK) to keep the
-- auth and user modules independent at the schema level, per the module boundary rules.

CREATE TABLE customers (
    id         UUID         NOT NULL,
    user_id    UUID         NOT NULL,
    email      VARCHAR(320) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    phone      VARCHAR(20),
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uq_customers_user_id UNIQUE (user_id)
);

CREATE TABLE customer_addresses (
    id          UUID         NOT NULL,
    customer_id UUID         NOT NULL,
    label       VARCHAR(50),
    line1       VARCHAR(200) NOT NULL,
    line2       VARCHAR(200),
    city        VARCHAR(100) NOT NULL,
    state       VARCHAR(100),
    postal_code VARCHAR(20)  NOT NULL,
    country     VARCHAR(100) NOT NULL,
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    CONSTRAINT pk_customer_addresses PRIMARY KEY (id),
    CONSTRAINT fk_customer_addresses_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
);

CREATE INDEX idx_customer_addresses_customer_id ON customer_addresses (customer_id);