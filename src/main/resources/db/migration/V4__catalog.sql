-- V4__catalog.sql
-- Catalog module schema: categories, brands, products and product images.
--
-- Products reference categories/brands by id within the catalog module. parent_category_id is a
-- self-reference for sub-categories. No FKs cross module boundaries.

CREATE TABLE categories (
    id                 UUID         NOT NULL,
    name               VARCHAR(150) NOT NULL,
    slug               VARCHAR(150) NOT NULL,
    description        VARCHAR(2000),
    parent_category_id UUID,
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    created_by         VARCHAR(255),
    updated_by         VARCHAR(255),
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uq_categories_slug UNIQUE (slug),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_category_id) REFERENCES categories (id)
);

CREATE INDEX idx_categories_parent_id ON categories (parent_category_id);

CREATE TABLE brands (
    id          UUID         NOT NULL,
    name        VARCHAR(150) NOT NULL,
    slug        VARCHAR(150) NOT NULL,
    description VARCHAR(2000),
    logo_url    VARCHAR(500),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    CONSTRAINT pk_brands PRIMARY KEY (id),
    CONSTRAINT uq_brands_slug UNIQUE (slug)
);

CREATE TABLE products (
    id          UUID           NOT NULL,
    name        VARCHAR(200)   NOT NULL,
    slug        VARCHAR(200)   NOT NULL,
    sku         VARCHAR(64)    NOT NULL,
    description VARCHAR(4000),
    price       NUMERIC(12, 2) NOT NULL,
    currency    VARCHAR(3)     NOT NULL,
    category_id UUID           NOT NULL,
    brand_id    UUID,
    active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ    NOT NULL,
    updated_at  TIMESTAMPTZ    NOT NULL,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uq_products_slug UNIQUE (slug),
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands (id)
);

CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_brand_id ON products (brand_id);
CREATE INDEX idx_products_active ON products (active);

CREATE TABLE product_images (
    id         UUID         NOT NULL,
    product_id UUID         NOT NULL,
    url        VARCHAR(500) NOT NULL,
    alt_text   VARCHAR(255),
    sort_order INTEGER      NOT NULL DEFAULT 0,
    is_primary BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT pk_product_images PRIMARY KEY (id),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_product_images_product_id ON product_images (product_id);