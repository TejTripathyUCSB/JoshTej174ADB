-- Inventory snapshot
SELECT stock_number, manufacturer_name, quantity, min_stock, max_stock, replenishment
FROM item ORDER BY stock_number;

-- Orders snapshot
SELECT order_id, customer_id, subtotal, discount, shipping_fee, total, fulfillment_status
FROM emart_orders ORDER BY order_id;

-- Customers snapshot
SELECT customer_id, status FROM emart_customers ORDER BY customer_id;

-- eDEPOT activity
SELECT notice_id, manufacturer_name, status FROM edepot_shipping_notices ORDER BY notice_id;
SELECT order_id,  manufacturer_name           FROM edepot_replenishment_orders ORDER BY order_id;