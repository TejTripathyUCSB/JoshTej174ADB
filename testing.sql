SELECT stock_number, quantity, min_stock, max_stock, replenishment FROM item ORDER BY stock_number;
SELECT * FROM edepot_replenishment_orders ORDER BY order_id;
SELECT * FROM edepot_replenishment_items ORDER BY order_id, stock_number;