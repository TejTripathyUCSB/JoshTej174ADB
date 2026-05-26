-- ===================================================================
-- CS 174A Project: Schema (eMART + eDEPOT)
-- ===================================================================
-- Conventions:
--   - Lowercase snake_case table/column names
--   - emart_* prefix for store-side tables
--   - edepot_* prefix for warehouse-side tables
--   - Lookup tables (manufacturer, category, etc.) have no prefix
--     since they're shared between both systems.
--
-- Run order:
--   1. Run 03_drop.sql first to clear any existing tables.
--   2. Run this file.
--   3. Hit COMMIT.
--   4. Run 02_seed.sql to populate sample data.
-- ===================================================================


-- ============================================================
-- PART 1.  SHARED LOOKUP TABLES
-- ============================================================

CREATE TABLE manufacturer (
    name VARCHAR(20) NOT NULL,
    PRIMARY KEY (name)
);

CREATE TABLE shipping_company (
    name VARCHAR(20) NOT NULL,
    PRIMARY KEY (name)
);

CREATE TABLE category (
    name VARCHAR(20) NOT NULL,
    PRIMARY KEY (name)
);


-- ============================================================
-- PART 2.  SHARED ITEM TABLE  (the bridge between eMart & eDepot)
-- ============================================================
-- One row per product TYPE. Owned by eDepot conceptually (eDepot
-- assigns stock_number on first receipt), but eMart's product table
-- references it. This is the ONLY place manufacturer + model live.
-- eMart queries that need manufacturer/model JOIN to this table.

CREATE TABLE item (
    stock_number      CHAR(7)     NOT NULL,
    manufacturer_name VARCHAR(20) NOT NULL,
    model_number      VARCHAR(20) NOT NULL,
    quantity          INTEGER     DEFAULT 0 NOT NULL,
    min_stock         INTEGER     DEFAULT 0 NOT NULL,
    max_stock         INTEGER     NOT NULL,
    location          VARCHAR(10) NOT NULL,
    replenishment     INTEGER     DEFAULT 0 NOT NULL,
    PRIMARY KEY (stock_number),
    UNIQUE (location),
    UNIQUE (manufacturer_name, model_number),
    FOREIGN KEY (manufacturer_name) REFERENCES manufacturer(name),
    CONSTRAINT chk_stock_num_format
        CHECK (REGEXP_LIKE(stock_number, '^[A-Z]{2}[0-9]{5}$')),
    CONSTRAINT chk_location_format
        CHECK (REGEXP_LIKE(UPPER(location), '^[A-Z][1-9][0-9]*$')),
    CONSTRAINT chk_qty_nonneg     CHECK (quantity      >= 0),
    CONSTRAINT chk_min_nonneg     CHECK (min_stock     >= 0),
    CONSTRAINT chk_max_geq_min    CHECK (max_stock     >= min_stock),
    CONSTRAINT chk_qty_leq_max    CHECK (quantity      <= max_stock),
    CONSTRAINT chk_replen_nonneg  CHECK (replenishment >= 0)
);


-- ============================================================
-- PART 3.  eMART CATALOG
-- ============================================================
-- emart_products: sales-side info ONLY.
-- 1:1 with item.stock_number. Manufacturer and model live in `item`;
-- queries that need them JOIN to item.

CREATE TABLE emart_products (
    stock_number  CHAR(7)        NOT NULL,
    category      VARCHAR(20)    NOT NULL,
    warranty      INTEGER        NOT NULL,    -- months
    price         NUMBER(10,2)   NOT NULL,
    PRIMARY KEY (stock_number),
    FOREIGN KEY (stock_number) REFERENCES item(stock_number),
    FOREIGN KEY (category)     REFERENCES category(name),
    CONSTRAINT chk_warranty_nonneg CHECK (warranty >= 0),
    CONSTRAINT chk_price_nonneg    CHECK (price    >= 0)
);

-- Multi-valued product description attributes (processor speed, RAM, etc.)
CREATE TABLE emart_product_attributes (
    stock_number    CHAR(7)     NOT NULL,
    attribute_name  VARCHAR(30) NOT NULL,
    attribute_value VARCHAR(50) NOT NULL,
    PRIMARY KEY (stock_number, attribute_name),
    FOREIGN KEY (stock_number) REFERENCES emart_products(stock_number) ON DELETE CASCADE
);

-- Product compatibility (directional: stock_number can replace compatible_stock_number).
-- For symmetric compatibility, insert both (X,Y) and (Y,X).
CREATE TABLE emart_compatibility (
    stock_number            CHAR(7) NOT NULL,
    compatible_stock_number CHAR(7) NOT NULL,
    PRIMARY KEY (stock_number, compatible_stock_number),
    FOREIGN KEY (stock_number)            REFERENCES emart_products(stock_number) ON DELETE CASCADE,
    FOREIGN KEY (compatible_stock_number) REFERENCES emart_products(stock_number),
    CONSTRAINT chk_compat_not_self CHECK (stock_number <> compatible_stock_number)
);


-- ============================================================
-- PART 4.  eMART CUSTOMERS AND CARTS
-- ============================================================

CREATE TABLE emart_customers (
    customer_id  VARCHAR(20)  NOT NULL,
    password     VARCHAR(50)  NOT NULL,
    first_name   VARCHAR(20)  NOT NULL,
    middle_name  VARCHAR(20),
    last_name    VARCHAR(20)  NOT NULL,
    email        VARCHAR(50)  NOT NULL,
    address      VARCHAR(100) NOT NULL,
    status       VARCHAR(10)  DEFAULT 'NEW' NOT NULL,
    PRIMARY KEY (customer_id),
    UNIQUE (email),
    CONSTRAINT chk_customer_status CHECK (status IN ('NEW', 'GREEN', 'SILVER', 'GOLD'))
);

CREATE TABLE emart_cart_items (
    customer_id  VARCHAR(20) NOT NULL,
    stock_number CHAR(7)     NOT NULL,
    quantity     INTEGER     NOT NULL,
    PRIMARY KEY (customer_id, stock_number),
    FOREIGN KEY (customer_id)  REFERENCES emart_customers(customer_id) ON DELETE CASCADE,
    FOREIGN KEY (stock_number) REFERENCES emart_products(stock_number),
    CONSTRAINT chk_cart_qty_pos CHECK (quantity > 0)
);


-- ============================================================
-- PART 5.  eMART ORDERS
-- ============================================================

-- fulfillment_status tracks whether eDepot has filled the order yet.
-- New orders default to PENDING; eDepot's fillOrder flips them to FILLED.
-- This is what lets fillOrder be a separate action: it can query for
-- pending orders and refuse to double-fill an already-filled order.
CREATE TABLE emart_orders (
    order_id            INTEGER       GENERATED BY DEFAULT AS IDENTITY,
    customer_id         VARCHAR(20)   NOT NULL,
    order_date          DATE          DEFAULT SYSDATE NOT NULL,
    subtotal            NUMBER(10,2)  NOT NULL,
    discount            NUMBER(10,2)  NOT NULL,
    shipping_fee        NUMBER(10,2)  NOT NULL,
    total               NUMBER(10,2)  NOT NULL,
    fulfillment_status  VARCHAR(10)   DEFAULT 'PENDING' NOT NULL,
    filled_date         DATE,
    PRIMARY KEY (order_id),
    FOREIGN KEY (customer_id) REFERENCES emart_customers(customer_id),
    CONSTRAINT chk_order_subtotal_nonneg CHECK (subtotal >= 0),
    CONSTRAINT chk_order_total_nonneg    CHECK (total    >= 0),
    CONSTRAINT chk_fulfillment_status    CHECK (fulfillment_status IN ('PENDING', 'FILLED'))
);

CREATE TABLE emart_order_items (
    order_id     INTEGER      NOT NULL,
    stock_number CHAR(7)      NOT NULL,
    quantity     INTEGER      NOT NULL,
    price_each   NUMBER(10,2) NOT NULL,
    PRIMARY KEY (order_id, stock_number),
    FOREIGN KEY (order_id)     REFERENCES emart_orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (stock_number) REFERENCES emart_products(stock_number),
    CONSTRAINT chk_oi_qty_pos      CHECK (quantity   > 0),
    CONSTRAINT chk_oi_price_nonneg CHECK (price_each >= 0)
);


-- ============================================================
-- PART 6.  eMART BUSINESS RULES
-- ============================================================

CREATE TABLE emart_rules (
    rule_name   VARCHAR(40)  NOT NULL,
    rule_value  NUMBER(10,2) NOT NULL,
    PRIMARY KEY (rule_name)
);


-- ============================================================
-- PART 7.  eDEPOT TABLES
-- ============================================================

CREATE TABLE edepot_shipping_notices (
    notice_id             VARCHAR(20) NOT NULL,
    manufacturer_name     VARCHAR(20) NOT NULL,
    shipping_company_name VARCHAR(20) NOT NULL,
    status                VARCHAR(10) DEFAULT 'PENDING' NOT NULL,
    notice_date           DATE        DEFAULT SYSDATE NOT NULL,
    received_date         DATE,
    PRIMARY KEY (notice_id),
    FOREIGN KEY (manufacturer_name)     REFERENCES manufacturer(name),
    FOREIGN KEY (shipping_company_name) REFERENCES shipping_company(name),
    CONSTRAINT chk_notice_status CHECK (status IN ('PENDING', 'RECEIVED'))
);

CREATE TABLE edepot_shipping_notice_items (
    notice_id    VARCHAR(20) NOT NULL,
    stock_number CHAR(7)     NOT NULL,
    quantity     INTEGER     NOT NULL,
    PRIMARY KEY (notice_id, stock_number),
    FOREIGN KEY (notice_id)    REFERENCES edepot_shipping_notices(notice_id) ON DELETE CASCADE,
    FOREIGN KEY (stock_number) REFERENCES item(stock_number),
    CONSTRAINT chk_sni_qty_pos CHECK (quantity > 0)
);

CREATE TABLE edepot_replenishment_orders (
    order_id          VARCHAR(20) NOT NULL,
    manufacturer_name VARCHAR(20) NOT NULL,
    order_date        DATE        DEFAULT SYSDATE NOT NULL,
    PRIMARY KEY (order_id),
    FOREIGN KEY (manufacturer_name) REFERENCES manufacturer(name)
);

CREATE TABLE edepot_replenishment_items (
    order_id     VARCHAR(20) NOT NULL,
    stock_number CHAR(7)     NOT NULL,
    quantity     INTEGER     NOT NULL,
    PRIMARY KEY (order_id, stock_number),
    FOREIGN KEY (order_id)     REFERENCES edepot_replenishment_orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (stock_number) REFERENCES item(stock_number),
    CONSTRAINT chk_ri_qty_pos CHECK (quantity > 0)
);


-- ============================================================
-- PART 8.  SEQUENCES
-- ============================================================
CREATE SEQUENCE shipping_notice_seq     START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE replenishment_order_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE stock_number_seq        START WITH 1 INCREMENT BY 1;


-- ============================================================
-- PART 9.  INITIAL RULE VALUES
-- ============================================================
INSERT INTO emart_rules VALUES ('NEW_CUSTOMER_DISCOUNT_PERCENT',    10);
INSERT INTO emart_rules VALUES ('GOLD_DISCOUNT_PERCENT',            10);
INSERT INTO emart_rules VALUES ('SILVER_DISCOUNT_PERCENT',           5);
INSERT INTO emart_rules VALUES ('GREEN_DISCOUNT_PERCENT',            0);
INSERT INTO emart_rules VALUES ('SHIPPING_PERCENT',                 10);
INSERT INTO emart_rules VALUES ('FREE_SHIPPING_THRESHOLD',         100);
INSERT INTO emart_rules VALUES ('SILVER_STATUS_THRESHOLD',         100);
INSERT INTO emart_rules VALUES ('GOLD_STATUS_THRESHOLD',           500);

COMMIT;