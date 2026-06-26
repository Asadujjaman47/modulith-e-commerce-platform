-- V12__review.sql
-- Review module schema: product reviews, an aggregate rating per product, and a purchase-eligibility
-- replica.
--
-- product_id references the catalog module and customer_id references the auth/user id by value only
-- (no cross-module FKs), per the module boundary rules. A customer may review a given product at most
-- once (uq_reviews_product_customer).
--
-- ratings holds one row per product: the running review count and rating sum, with the derived
-- average maintained by the review module as reviews are created/deleted.
--
-- review_eligibility is a local replica populated from OrderCompletedEvent: a customer becomes
-- eligible to write reviews once they have at least one delivered order.

CREATE TABLE reviews (
    id          UUID         NOT NULL,
    product_id  UUID         NOT NULL,
    customer_id UUID         NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    rating      INTEGER      NOT NULL,
    title       VARCHAR(150),
    comment     VARCHAR(2000),
    status      VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    CONSTRAINT pk_reviews PRIMARY KEY (id),
    CONSTRAINT uq_reviews_product_customer UNIQUE (product_id, customer_id),
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_reviews_product_id ON reviews (product_id);
CREATE INDEX idx_reviews_customer_id ON reviews (customer_id);

CREATE TABLE ratings (
    id             UUID          NOT NULL,
    product_id     UUID          NOT NULL,
    review_count   INTEGER       NOT NULL,
    rating_sum     BIGINT        NOT NULL,
    average_rating NUMERIC(3, 2) NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL,
    created_by     VARCHAR(255),
    updated_by     VARCHAR(255),
    CONSTRAINT pk_ratings PRIMARY KEY (id),
    CONSTRAINT uq_ratings_product UNIQUE (product_id)
);

CREATE TABLE review_eligibility (
    id          UUID        NOT NULL,
    customer_id UUID        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    CONSTRAINT pk_review_eligibility PRIMARY KEY (id),
    CONSTRAINT uq_review_eligibility_customer UNIQUE (customer_id)
);
