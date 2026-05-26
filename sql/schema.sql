

BEGIN EXECUTE IMMEDIATE 'DROP TABLE emart_order_items CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE emart_orders CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE emart_cart_items CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE emart_compatibility CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE emart_product_attributes CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE emart_products CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE emart_customers CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE emart_rules CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE edepot_replenishment_items CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE edepot_replenishment_orders CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE edepot_shipping_notice_items CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE edepot_shipping_notices CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE edepot_inventory CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- ============================================================
-- eMART: customers
-- Stores login/customer identity and customer discount status.
-- Status comes from last three purchases:
-- NEW, GREEN, SILVER, GOLD
-- ============================================================

CREATE TABLE emart_customers (
    customer_id     VARCHAR2(20),
    password        VARCHAR2(100) NOT NULL,
    customer_name   VARCHAR2(100) NOT NULL,
    email           VARCHAR2(100) UNIQUE,
    address         VARCHAR2(200),
    status          VARCHAR2(20) DEFAULT 'NEW',

    CONSTRAINT pk_emart_customers PRIMARY KEY (customer_id),
    CONSTRAINT chk_customer_status CHECK (status IN ('NEW', 'GREEN', 'SILVER', 'GOLD'))
);

-- ============================================================
-- eMART: products/catalog
-- This is the product catalog customers search from.
-- Stock number format should be XXnnnnn, like AB12345.
-- ============================================================

CREATE TABLE emart_products (
    stock_number        VARCHAR2(7),
    category            VARCHAR2(20) NOT NULL,
    manufacturer        VARCHAR2(20) NOT NULL,
    model_number        VARCHAR2(20) NOT NULL,
    product_description VARCHAR2(4000),
    warranty_months     NUMBER DEFAULT 0,
    price               NUMBER(10,2) NOT NULL,

    CONSTRAINT pk_emart_products PRIMARY KEY (stock_number),
    CONSTRAINT uq_emart_product_model UNIQUE (manufacturer, model_number),
    CONSTRAINT chk_stock_number_format CHECK (REGEXP_LIKE(stock_number, '^[A-Z]{2}[0-9]{5}$')),
    CONSTRAINT chk_product_price CHECK (price >= 0),
    CONSTRAINT chk_warranty CHECK (warranty_months >= 0)
);

-- ============================================================
-- eMART: product attributes
-- Stores description as attribute-value pairs.
-- Example: processor_speed = 2.5GHz, memory = 16GB
-- This satisfies the spec better than only one description string.
-- ============================================================

CREATE TABLE emart_product_attributes (
    stock_number     VARCHAR2(7),
    attribute_name   VARCHAR2(20),
    attribute_value  VARCHAR2(100),

    CONSTRAINT pk_emart_product_attributes PRIMARY KEY (stock_number, attribute_name),
    CONSTRAINT fk_attr_product FOREIGN KEY (stock_number)
        REFERENCES emart_products(stock_number)
        ON DELETE CASCADE
);

-- ============================================================
-- eMART: compatibility
-- A product can replace/be compatible with another manufacturer+model.
-- ============================================================

CREATE TABLE emart_compatibility (
    stock_number              VARCHAR2(7),
    compatible_manufacturer   VARCHAR2(20),
    compatible_model_number   VARCHAR2(20),

    CONSTRAINT pk_emart_compatibility PRIMARY KEY
        (stock_number, compatible_manufacturer, compatible_model_number),

    CONSTRAINT fk_compat_product FOREIGN KEY (stock_number)
        REFERENCES emart_products(stock_number)
        ON DELETE CASCADE
);

-- ============================================================
-- eMART: rules
-- Stores discounts and shipping policies in DB because spec says
-- these values may change.
-- ============================================================

CREATE TABLE emart_rules (
    rule_name   VARCHAR2(50),
    rule_value  NUMBER(10,2) NOT NULL,

    CONSTRAINT pk_emart_rules PRIMARY KEY (rule_name),
    CONSTRAINT chk_rule_value CHECK (rule_value >= 0)
);

-- ============================================================
-- eMART: cart items
-- This stores current shopping cart contents.
-- Required operations: add item, delete item, display cart.
-- ============================================================

CREATE TABLE emart_cart_items (
    customer_id   VARCHAR2(20),
    stock_number  VARCHAR2(7),
    quantity      NUMBER NOT NULL,

    CONSTRAINT pk_emart_cart_items PRIMARY KEY (customer_id, stock_number),

    CONSTRAINT fk_cart_customer FOREIGN KEY (customer_id)
        REFERENCES emart_customers(customer_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_cart_product FOREIGN KEY (stock_number)
        REFERENCES emart_products(stock_number)
        ON DELETE CASCADE,

    CONSTRAINT chk_cart_quantity CHECK (quantity > 0)
);

-- ============================================================
-- eMART: orders
-- One row per checkout.
-- Stores subtotal, discount, shipping fee, final total.
-- ============================================================

CREATE TABLE emart_orders (
    order_id            NUMBER GENERATED BY DEFAULT AS IDENTITY,
    customer_id         VARCHAR2(20) NOT NULL,
    order_date          DATE DEFAULT SYSDATE,

    subtotal            NUMBER(10,2) NOT NULL,
    discount            NUMBER(10,2) DEFAULT 0,
    shipping_fee        NUMBER(10,2) DEFAULT 0,
    total               NUMBER(10,2) NOT NULL,

    fulfillment_status  VARCHAR2(20) DEFAULT 'PENDING',
    filled_date         DATE,

    CONSTRAINT pk_emart_orders PRIMARY KEY (order_id),

    CONSTRAINT fk_order_customer FOREIGN KEY (customer_id)
        REFERENCES emart_customers(customer_id),

    CONSTRAINT chk_order_money CHECK (
        subtotal >= 0
        AND discount >= 0
        AND shipping_fee >= 0
        AND total >= 0
    ),

    CONSTRAINT chk_fulfillment_status CHECK (
        fulfillment_status IN ('PENDING','FILLED')
    )
);

-- ============================================================
-- eMART: order items
-- Items inside each order.
-- Needed for displaying previous orders and rerunning orders.
-- ============================================================

CREATE TABLE emart_order_items (
    order_id      NUMBER,
    stock_number  VARCHAR2(7),
    quantity      NUMBER NOT NULL,
    price_each    NUMBER(10,2) NOT NULL,

    CONSTRAINT pk_emart_order_items PRIMARY KEY (order_id, stock_number),

    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES emart_orders(order_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_order_items_product FOREIGN KEY (stock_number)
        REFERENCES emart_products(stock_number),

    CONSTRAINT chk_order_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_item_price CHECK (price_each >= 0)
);

-- ============================================================
-- eDEPOT: inventory
-- Warehouse quantity data.
-- eMART product stock_number links to eDEPOT inventory stock_number.
-- ============================================================

CREATE TABLE edepot_inventory (
    stock_number     VARCHAR2(7),
    manufacturer     VARCHAR2(20) NOT NULL,
    model_number     VARCHAR2(20) NOT NULL,
    quantity         NUMBER DEFAULT 0,
    minimum_stock    NUMBER DEFAULT 0,
    maximum_stock    NUMBER DEFAULT 0,
    location         VARCHAR2(20),
    replenishment    NUMBER DEFAULT 0,

    CONSTRAINT pk_edepot_inventory PRIMARY KEY (stock_number),
    CONSTRAINT uq_edepot_model UNIQUE (manufacturer, model_number),

    CONSTRAINT fk_inventory_product FOREIGN KEY (stock_number)
        REFERENCES emart_products(stock_number),

    CONSTRAINT chk_inventory_stock_number CHECK (REGEXP_LIKE(stock_number, '^[A-Z]{2}[0-9]{5}$')),
    CONSTRAINT chk_inventory_quantities CHECK (
    quantity >= 0
    AND minimum_stock >= 0
    AND maximum_stock >= 0
    AND replenishment >= 0
    AND quantity <= maximum_stock
    ),
    CONSTRAINT chk_inventory_min_max CHECK (minimum_stock <= maximum_stock),
    CONSTRAINT chk_inventory_location CHECK (REGEXP_LIKE(location, '^[A-Za-z][1-9][0-9]*$'))
);

-- ============================================================
-- eDEPOT: shipping notices
-- Manufacturer says: "items are coming."
-- This increases replenishment, not actual quantity yet.
-- ============================================================

CREATE TABLE edepot_shipping_notices (
    notice_id         VARCHAR2(30),
    shipping_company  VARCHAR2(20) NOT NULL,
    notice_date       DATE DEFAULT SYSDATE,
    received_status   VARCHAR2(20) DEFAULT 'PENDING',

    CONSTRAINT pk_shipping_notices PRIMARY KEY (notice_id),
    CONSTRAINT chk_notice_status CHECK (received_status IN ('PENDING', 'RECEIVED'))
);

-- ============================================================
-- eDEPOT: shipping notice items
-- Items listed inside a shipping notice.
-- ============================================================

CREATE TABLE edepot_shipping_notice_items (
    notice_id      VARCHAR2(30),
    manufacturer   VARCHAR2(20),
    model_number   VARCHAR2(20),
    quantity       NUMBER NOT NULL,

    CONSTRAINT pk_shipping_notice_items PRIMARY KEY
        (notice_id, manufacturer, model_number),

    CONSTRAINT fk_notice_items_notice FOREIGN KEY (notice_id)
        REFERENCES edepot_shipping_notices(notice_id)
        ON DELETE CASCADE,

    CONSTRAINT chk_notice_item_quantity CHECK (quantity > 0)
);

-- ============================================================
-- eDEPOT: replenishment orders
-- Created when inventory drops below min stock.
-- Spec says if 3+ products from same manufacturer fall below min,
-- send order containing products below max.
-- ============================================================

CREATE TABLE edepot_replenishment_orders (
    replenishment_id  NUMBER GENERATED BY DEFAULT AS IDENTITY,
    manufacturer      VARCHAR2(20) NOT NULL,
    order_date        DATE DEFAULT SYSDATE,
    status            VARCHAR2(20) DEFAULT 'CREATED',

    CONSTRAINT pk_replenishment_orders PRIMARY KEY (replenishment_id),
    CONSTRAINT chk_replenishment_status CHECK (status IN ('CREATED', 'SENT', 'RECEIVED'))
);

-- ============================================================
-- eDEPOT: replenishment items
-- Items inside each replenishment order.
-- ============================================================

CREATE TABLE edepot_replenishment_items (
    replenishment_id  NUMBER,
    stock_number      VARCHAR2(7),
    quantity          NUMBER NOT NULL,

    CONSTRAINT pk_replenishment_items PRIMARY KEY (replenishment_id, stock_number),

    CONSTRAINT fk_replenishment_item_order FOREIGN KEY (replenishment_id)
        REFERENCES edepot_replenishment_orders(replenishment_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_replenishment_item_inventory FOREIGN KEY (stock_number)
        REFERENCES edepot_inventory(stock_number),

    CONSTRAINT chk_replenishment_item_quantity CHECK (quantity > 0)
);

-- ============================================================
-- DEFAULT RULES
-- These are stored in DB so manager can change them later.
-- ============================================================

INSERT INTO emart_rules VALUES ('NEW_CUSTOMER_DISCOUNT_PERCENT', 10);
INSERT INTO emart_rules VALUES ('GOLD_DISCOUNT_PERCENT', 10);
INSERT INTO emart_rules VALUES ('SILVER_DISCOUNT_PERCENT', 5);
INSERT INTO emart_rules VALUES ('GREEN_DISCOUNT_PERCENT', 0);
INSERT INTO emart_rules VALUES ('SHIPPING_PERCENT', 10);
INSERT INTO emart_rules VALUES ('FREE_SHIPPING_THRESHOLD', 100);
INSERT INTO emart_rules VALUES ('SILVER_STATUS_THRESHOLD', 100);
INSERT INTO emart_rules VALUES ('GOLD_STATUS_THRESHOLD', 500);

COMMIT;
