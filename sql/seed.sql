
DELETE FROM edepot_replenishment_items;
DELETE FROM edepot_replenishment_orders;
DELETE FROM edepot_shipping_notice_items;
DELETE FROM edepot_shipping_notices;
DELETE FROM edepot_inventory;
DELETE FROM emart_order_items;
DELETE FROM emart_orders;
DELETE FROM emart_cart_items;
DELETE FROM emart_compatibility;
DELETE FROM emart_product_attributes;
DELETE FROM emart_products;
DELETE FROM emart_customers;

-- ============================================================
-- eMART CUSTOMERS
-- Status matters for discount logic:
-- NEW/GOLD = 10%, SILVER = 5%, GREEN = 0%
-- ============================================================

INSERT INTO emart_customers
VALUES ('C001', 'pass123', 'Alice Kim', 'alice@test.com', '1 Main St', 'NEW');

INSERT INTO emart_customers
VALUES ('C002', 'pass123', 'Ben Lee', 'ben@test.com', '2 Ocean Ave', 'SILVER');

INSERT INTO emart_customers
VALUES ('C003', 'pass123', 'Cara Fox', 'cara@test.com', '3 State St', 'GOLD');

INSERT INTO emart_customers
VALUES ('C004', 'pass123', 'Dan Wu', 'dan@test.com', '4 Mesa Rd', 'GREEN');

-- ============================================================
-- eMART PRODUCTS / CATALOG
-- These are searchable by stock number, manufacturer, model,
-- category, and attributes.
-- ============================================================

INSERT INTO emart_products
VALUES ('CP00001', 'computers', 'Dell', 'XPS13', 'Laptop computer', 12, 999.99);

INSERT INTO emart_products
VALUES ('HD00001', 'harddisks', 'Seagate', 'B2TB', 'External hard disk', 24, 89.99);

INSERT INTO emart_products
VALUES ('MN00001', 'monitors', 'Samsung', 'S24', '24 inch monitor', 12, 149.99);

INSERT INTO emart_products
VALUES ('KB00001', 'keyboards', 'Logitech', 'K380', 'Wireless keyboard', 12, 39.99);

INSERT INTO emart_products
VALUES ('MS00001', 'mice', 'Logitech', 'M185', 'Wireless mouse', 12, 19.99);

INSERT INTO emart_products
VALUES ('CP00002', 'computers', 'Apple', 'MBA13', 'Laptop computer', 12, 1199.99);

-- ============================================================
-- PRODUCT ATTRIBUTES
-- This supports searching by description attribute/value.
-- ============================================================

INSERT INTO emart_product_attributes VALUES ('CP00001', 'processor', 'i7');
INSERT INTO emart_product_attributes VALUES ('CP00001', 'memory', '16GB');
INSERT INTO emart_product_attributes VALUES ('CP00001', 'storage', '512GB');

INSERT INTO emart_product_attributes VALUES ('CP00002', 'processor', 'M3');
INSERT INTO emart_product_attributes VALUES ('CP00002', 'memory', '8GB');
INSERT INTO emart_product_attributes VALUES ('CP00002', 'storage', '256GB');

INSERT INTO emart_product_attributes VALUES ('HD00001', 'capacity', '2TB');
INSERT INTO emart_product_attributes VALUES ('MN00001', 'size', '24in');
INSERT INTO emart_product_attributes VALUES ('KB00001', 'wireless', 'yes');
INSERT INTO emart_product_attributes VALUES ('MS00001', 'wireless', 'yes');

-- ============================================================
-- COMPATIBILITY
-- This supports "find compatible items" operation.
-- ============================================================

INSERT INTO emart_compatibility VALUES ('KB00001', 'Logitech', 'K120');
INSERT INTO emart_compatibility VALUES ('MS00001', 'Logitech', 'M100');
INSERT INTO emart_compatibility VALUES ('HD00001', 'Seagate', 'B1TB');

-- ============================================================
-- eDEPOT INVENTORY
-- Same stock numbers as products.
-- Quantity changes when orders are filled.
-- ============================================================

INSERT INTO edepot_inventory
VALUES ('CP00001', 'Dell', 'XPS13', 10, 3, 20, 'A1', 0);

INSERT INTO edepot_inventory
VALUES ('HD00001', 'Seagate', 'B2TB', 25, 5, 40, 'B2', 0);

INSERT INTO edepot_inventory
VALUES ('MN00001', 'Samsung', 'S24', 15, 4, 30, 'C3', 0);

INSERT INTO edepot_inventory
VALUES ('KB00001', 'Logitech', 'K380', 8, 5, 25, 'D4', 0);

INSERT INTO edepot_inventory
VALUES ('MS00001', 'Logitech', 'M185', 7, 5, 25, 'D5', 0);

INSERT INTO edepot_inventory
VALUES ('CP00002', 'Apple', 'MBA13', 6, 2, 15, 'A2', 0);

COMMIT;