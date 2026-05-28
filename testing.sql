SELECT * FROM edepot_shipping_notices WHERE notice_id = 'N_BAD';     -- 0 rows
SELECT * FROM edepot_shipping_notice_items WHERE notice_id = 'N_BAD'; -- 0 rows
SELECT replenishment FROM item WHERE stock_number = 'AA00201';       -- unchanged