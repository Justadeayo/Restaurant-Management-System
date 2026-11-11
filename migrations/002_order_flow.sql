USE restaurant_management;

-- Allow both old and new status values while existing rows are converted
ALTER TABLE orders
    MODIFY status ENUM('PENDING', 'COMPLETED', 'CANCELLED', 'PLACED', 'SENT_TO_KITCHEN', 'READY', 'OUT_FOR_DELIVERY', 'DELIVERED', 'PAID') NOT NULL DEFAULT 'PENDING';

UPDATE orders SET status = 'PLACED' WHERE status = 'PENDING';
UPDATE orders SET status = 'PAID' WHERE status = 'COMPLETED';

ALTER TABLE orders
    MODIFY status ENUM('PLACED', 'SENT_TO_KITCHEN', 'READY', 'OUT_FOR_DELIVERY', 'DELIVERED', 'PAID', 'CANCELLED') NOT NULL DEFAULT 'PLACED';

ALTER TABLE orders
    ADD COLUMN order_type ENUM('TABLE', 'REMOTE') NOT NULL DEFAULT 'TABLE' AFTER cashier_id,
    ADD COLUMN table_number VARCHAR(20) NULL,
    ADD COLUMN delivery_address VARCHAR(255) NULL;
