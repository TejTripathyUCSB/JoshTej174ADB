-- ===================================================================
-- Seed data: sample manufacturers, categories, items, products,
-- and one test customer. Enough to run the ProjectName demo.
-- Run AFTER 01_schema.sql. Hit COMMIT after.
-- ===================================================================

-- Lookup tables
INSERT INTO manufacturer VALUES ('Logitech');
INSERT INTO manufacturer VALUES ('Apple');
INSERT INTO manufacturer VALUES ('Samsung');

INSERT INTO shipping_company VALUES ('UPS');
INSERT INTO shipping_company VALUES ('FedEx');
INSERT INTO shipping_company VALUES ('USPS');

INSERT INTO category VALUES ('computers');
INSERT INTO category VALUES ('keyboards');
INSERT INTO category VALUES ('monitors');

-- Items (eDepot's inventory records)
INSERT INTO item (stock_number, manufacturer_name, model_number,
                  quantity, min_stock, max_stock, location)
VALUES ('CP00001', 'Apple', 'MacBook-Pro-14', 10, 2, 50, 'A1');

INSERT INTO item (stock_number, manufacturer_name, model_number,
                  quantity, min_stock, max_stock, location)
VALUES ('KB00001', 'Logitech', 'MX-Keys-S', 25, 5, 100, 'B3');

INSERT INTO item (stock_number, manufacturer_name, model_number,
                  quantity, min_stock, max_stock, location)
VALUES ('MN00001', 'Samsung', 'Odyssey-G7', 8, 2, 30, 'C2');

-- Products (eMart catalog entries)
INSERT INTO emart_products VALUES ('CP00001', 'computers',  12, 1999.00);
INSERT INTO emart_products VALUES ('KB00001', 'keyboards',  24,  119.99);
INSERT INTO emart_products VALUES ('MN00001', 'monitors',   24,  599.99);

-- Product attributes
INSERT INTO emart_product_attributes VALUES ('CP00001', 'processor speed', '3.5GHz');
INSERT INTO emart_product_attributes VALUES ('CP00001', 'RAM',             '16GB');
INSERT INTO emart_product_attributes VALUES ('CP00001', 'storage',         '512GB SSD');
INSERT INTO emart_product_attributes VALUES ('KB00001', 'layout',          'full-size');
INSERT INTO emart_product_attributes VALUES ('KB00001', 'connection',      'wireless');
INSERT INTO emart_product_attributes VALUES ('MN00001', 'size',            '32-inch');
INSERT INTO emart_product_attributes VALUES ('MN00001', 'resolution',      '4K');

-- A test customer matching what ProjectName.main() uses
INSERT INTO emart_customers (customer_id, password, first_name, last_name,
                             email, address, status)
VALUES ('C001', 'password123', 'Test', 'User',
        'test@example.com', '123 Main St', 'NEW');

COMMIT;
