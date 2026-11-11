USE restaurant_management;

-- Allow the old and new values while rows are converted
ALTER TABLE orders
    MODIFY status ENUM('PLACED', 'SENT_TO_KITCHEN', 'READY', 'OUT_FOR_DELIVERY', 'DELIVERED', 'PAID', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PLACED';

-- Old paid and delivered orders are finished now
UPDATE orders SET status = 'COMPLETED' WHERE status IN ('PAID', 'DELIVERED');

ALTER TABLE orders
    MODIFY status ENUM('PLACED', 'SENT_TO_KITCHEN', 'READY', 'OUT_FOR_DELIVERY', 'PAID', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PLACED';
