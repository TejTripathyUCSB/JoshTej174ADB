-- ===================================================================
-- Seed data generated from SampleData.xlsx (products + customers).
-- Run AFTER 01_schema.sql. Hit COMMIT after.
-- ===================================================================

-- Lookup tables
INSERT INTO manufacturer VALUES ('Canon');
INSERT INTO manufacturer VALUES ('Dell');
INSERT INTO manufacturer VALUES ('Envision');
INSERT INTO manufacturer VALUES ('HP');
INSERT INTO manufacturer VALUES ('McAfee');
INSERT INTO manufacturer VALUES ('Oracle');
INSERT INTO manufacturer VALUES ('Samsung');
INSERT INTO manufacturer VALUES ('Symantec');
INSERT INTO manufacturer VALUES ('eMachines');

INSERT INTO shipping_company VALUES ('UPS');
INSERT INTO shipping_company VALUES ('FedEx');
INSERT INTO shipping_company VALUES ('USPS');

INSERT INTO category VALUES ('Camera');
INSERT INTO category VALUES ('Desktop');
INSERT INTO category VALUES ('Laptop');
INSERT INTO category VALUES ('Monitor');
INSERT INTO category VALUES ('Printer');
INSERT INTO category VALUES ('Software');

-- Items (eDepot inventory records)
INSERT INTO item (stock_number, manufacturer_name, model_number, quantity, min_stock, max_stock, location, replenishment) VALUES ('AA00101', 'HP', 'A6111', 2, 1, 2, 'A9', 0);
INSERT INTO item (stock_number, manufacturer_name, model_number, quantity, min_stock, max_stock, location, replenishment) VALUES ('AA00201', 'Dell', 'B420', 3, 2, 5, 'A7', 0);
INSERT INTO item (stock_number, manufacturer_name, model_number, quantity, min_stock, max_stock, location, replenishment) VALUES ('AA00202', 'eMachines', 'C3958', 4, 2, 5, 'B52', 0);
INSERT INTO item (stock_number, manufacturer_name, model_number, quantity, min_stock, max_stock, location, replenishment) VALUES ('AA00301', 'Envision', 'D720', 4, 3, 6, 'C27', 0);
INSERT INTO item (stock_number, manufacturer_name, model_number, quantity, min_stock, max_stock, location, replenishment) VALUES ('AA00302', 'Samsung', 'E712', 5, 3, 6, 'C13', 0);
INSERT INTO item (stock_number, manufacturer_name, model_number, quantity, min_stock, max_stock, location, replenishment) VALUES ('AA00401', 'Symantec', 'F2005', 7, 5, 9, 'D27', 0);
INSERT INTO item (stock_number, manufacturer_name, model_number, quantity, min_stock, max_stock, location, replenishment) VALUES ('AA00402', 'McAfee', 'G2005', 7, 5, 9, 'D15', 0);
INSERT INTO item (stock_number, manufacturer_name, model_number, quantity, min_stock, max_stock, location, replenishment) VALUES ('AA00403', 'Oracle', 'H26', 7, 5, 9, 'D3', 0);
INSERT INTO item (stock_number, manufacturer_name, model_number, quantity, min_stock, max_stock, location, replenishment) VALUES ('AA00501', 'HP', 'J1320', 3, 2, 4, 'E7', 0);
INSERT INTO item (stock_number, manufacturer_name, model_number, quantity, min_stock, max_stock, location, replenishment) VALUES ('AA00601', 'HP', 'K435', 3, 2, 5, 'F9', 0);
INSERT INTO item (stock_number, manufacturer_name, model_number, quantity, min_stock, max_stock, location, replenishment) VALUES ('AA00602', 'Canon', 'L738', 3, 2, 5, 'F3', 0);

-- Products (eMart catalog entries)
INSERT INTO emart_products (stock_number, category, warranty, price) VALUES ('AA00101', 'Laptop', 12, 1630.00);
INSERT INTO emart_products (stock_number, category, warranty, price) VALUES ('AA00201', 'Desktop', 12, 239.00);
INSERT INTO emart_products (stock_number, category, warranty, price) VALUES ('AA00202', 'Desktop', 12, 369.99);
INSERT INTO emart_products (stock_number, category, warranty, price) VALUES ('AA00301', 'Monitor', 36, 69.99);
INSERT INTO emart_products (stock_number, category, warranty, price) VALUES ('AA00302', 'Monitor', 36, 279.99);
INSERT INTO emart_products (stock_number, category, warranty, price) VALUES ('AA00401', 'Software', 60, 19.99);
INSERT INTO emart_products (stock_number, category, warranty, price) VALUES ('AA00402', 'Software', 60, 19.99);
INSERT INTO emart_products (stock_number, category, warranty, price) VALUES ('AA00403', 'Software', 12, 29.99);
INSERT INTO emart_products (stock_number, category, warranty, price) VALUES ('AA00501', 'Printer', 12, 299.99);
INSERT INTO emart_products (stock_number, category, warranty, price) VALUES ('AA00601', 'Camera', 3, 119.99);
INSERT INTO emart_products (stock_number, category, warranty, price) VALUES ('AA00602', 'Camera', 1, 329.99);

-- Product attributes
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00101', 'Processor speed', '3.33Ghz');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00101', 'Ram size', '512 Mb');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00101', 'Hard disk size', '100Gb');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00101', 'Display Size', '17"');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00201', 'Processor speed', '2.53Ghz');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00201', 'Ram size', '256 Mb');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00201', 'Hard disk size', '80Gb');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00201', 'OS', 'none');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00202', 'Processor speed', '2.9Ghz');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00202', 'Ram size', '512 Mb');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00202', 'Hard disk size', '80Gb');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00301', 'Size', '17"');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00301', 'Weight', '25 lb.');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00302', 'Size', '17"');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00302', 'Weight', '9.6 lb.');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00401', 'Required disk size', '128 MB');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00401', 'Required RAM size', '64 MB');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00402', 'Required disk size', '128 MB');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00402', 'Required RAM size', '64 MB');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00403', 'Required disk size', '1 GB');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00403', 'Required RAM size', '128 MB');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00501', 'Resolution', '1200 dpi');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00501', 'Sheet capacity', '500');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00501', 'Weight', '.4 lb');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00601', 'Resolution', '3.1 Mp');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00601', 'Max zoom', '5 times');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00601', 'Weight', '24.7 lb');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00602', 'Resolution', '3.1 Mp');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00602', 'Max zoom', '5 times');
INSERT INTO emart_product_attributes (stock_number, attribute_name, attribute_value) VALUES ('AA00602', 'Weight', '24.7 lb');

-- Product compatibility (directional)
INSERT INTO emart_compatibility (stock_number, compatible_stock_number) VALUES ('AA00301', 'AA00201');
INSERT INTO emart_compatibility (stock_number, compatible_stock_number) VALUES ('AA00301', 'AA00202');
INSERT INTO emart_compatibility (stock_number, compatible_stock_number) VALUES ('AA00302', 'AA00201');
INSERT INTO emart_compatibility (stock_number, compatible_stock_number) VALUES ('AA00302', 'AA00202');
INSERT INTO emart_compatibility (stock_number, compatible_stock_number) VALUES ('AA00401', 'AA00101');
INSERT INTO emart_compatibility (stock_number, compatible_stock_number) VALUES ('AA00401', 'AA00201');
INSERT INTO emart_compatibility (stock_number, compatible_stock_number) VALUES ('AA00401', 'AA00202');
INSERT INTO emart_compatibility (stock_number, compatible_stock_number) VALUES ('AA00402', 'AA00101');
INSERT INTO emart_compatibility (stock_number, compatible_stock_number) VALUES ('AA00402', 'AA00201');
INSERT INTO emart_compatibility (stock_number, compatible_stock_number) VALUES ('AA00402', 'AA00202');
INSERT INTO emart_compatibility (stock_number, compatible_stock_number) VALUES ('AA00403', 'AA00101');
INSERT INTO emart_compatibility (stock_number, compatible_stock_number) VALUES ('AA00403', 'AA00201');
INSERT INTO emart_compatibility (stock_number, compatible_stock_number) VALUES ('AA00403', 'AA00202');
INSERT INTO emart_compatibility (stock_number, compatible_stock_number) VALUES ('AA00501', 'AA00201');
INSERT INTO emart_compatibility (stock_number, compatible_stock_number) VALUES ('AA00501', 'AA00202');
INSERT INTO emart_compatibility (stock_number, compatible_stock_number) VALUES ('AA00601', 'AA00201');
INSERT INTO emart_compatibility (stock_number, compatible_stock_number) VALUES ('AA00601', 'AA00202');
INSERT INTO emart_compatibility (stock_number, compatible_stock_number) VALUES ('AA00602', 'AA00201');
INSERT INTO emart_compatibility (stock_number, compatible_stock_number) VALUES ('AA00602', 'AA00202');

-- Customers
INSERT INTO emart_customers (customer_id, password, first_name, middle_name, last_name, email, address, status, is_manager) VALUES ('Lkim', 'Lkim', 'Linda', NULL, 'Kim', 'lkim@cs', '45 Oak Ave, Santa Barbara, CA 93101', 'GOLD', 'F');
INSERT INTO emart_customers (customer_id, password, first_name, middle_name, last_name, email, address, status, is_manager) VALUES ('Djones', 'Djones', 'Derek', NULL, 'Jones', 'djones@cs', '88 Pine St, Goleta, CA 93117', 'SILVER', 'F');
INSERT INTO emart_customers (customer_id, password, first_name, middle_name, last_name, email, address, status, is_manager) VALUES ('Mramirez', 'Mramirez', 'Maria', NULL, 'Ramirez', 'mramirez@cs', '12 Maple Rd, Carpinteria, CA 93013', 'NEW', 'F');
INSERT INTO emart_customers (customer_id, password, first_name, middle_name, last_name, email, address, status, is_manager) VALUES ('Tpatel', 'Tpatel', 'Tariq', NULL, 'Patel', 'tpatel@ce', '305 Elm Blvd, Ventura, CA 93001', 'NEW', 'F');
INSERT INTO emart_customers (customer_id, password, first_name, middle_name, last_name, email, address, status, is_manager) VALUES ('Swong', 'Swong', 'Sarah', NULL, 'Wong', 'swong@ce', '77 Cedar Lane, Ojai, CA 93023', 'GREEN', 'T');
INSERT INTO emart_customers (customer_id, password, first_name, middle_name, last_name, email, address, status, is_manager) VALUES ('Bford', 'Bford', 'Blake', NULL, 'Ford', 'bford@ce', '200 Spruce Ct, Oxnard, CA 93030', 'GREEN', 'F');
INSERT INTO emart_customers (customer_id, password, first_name, middle_name, last_name, email, address, status, is_manager) VALUES ('Tcodd', 'Tcodd', 'Ted', NULL, 'Codd', 'tcodd@db', '123 Database St, Data, CA 93116', 'GOLD', 'T');
INSERT INTO emart_customers (customer_id, password, first_name, middle_name, last_name, email, address, status, is_manager) VALUES ('Pchen', 'Pchen', 'Peter', NULL, 'Chen', 'pchen@db', '456 Database Wy, Datum, CA 93117', 'SILVER', 'F');
INSERT INTO emart_customers (customer_id, password, first_name, middle_name, last_name, email, address, status, is_manager) VALUES ('Jgray', 'Jgray', 'Jim', NULL, 'Gray', 'jgray@db', '789 Database Rd, Datas, CA 93118', 'GREEN', 'F');
INSERT INTO emart_customers (customer_id, password, first_name, middle_name, last_name, email, address, status, is_manager) VALUES ('Dknuth', 'Dknuth', 'Donald', NULL, 'Knuth', 'dknuth@cs', '101 Compsci Ln, Comp, CA 94305', 'GOLD', 'F');

COMMIT;
