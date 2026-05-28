-- ============================================================
-- 04_add_login.sql
-- Adds the is_manager column to emart_customers and marks
-- the demo manager(s). Idempotent: safe to run on an already-
-- loaded database.
-- Run this once if your DB was created before the GUI login.
-- ============================================================

-- Add the is_manager column if it isn't already there.
DECLARE
    col_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO col_count
    FROM   user_tab_columns
    WHERE  table_name  = 'EMART_CUSTOMERS'
      AND  column_name = 'IS_MANAGER';

    IF col_count = 0 THEN
        EXECUTE IMMEDIATE
            'ALTER TABLE emart_customers ADD ( ' ||
            'is_manager CHAR(1) DEFAULT ''F'' NOT NULL ' ||
            'CONSTRAINT chk_customer_is_manager CHECK (is_manager IN (''T'',''F'')) )';
    END IF;
END;
/

-- Promote demo manager(s).
UPDATE emart_customers SET is_manager = 'T' WHERE customer_id = 'Lkim';
UPDATE emart_customers SET is_manager = 'T' WHERE customer_id = 'Tcodd';

COMMIT;
