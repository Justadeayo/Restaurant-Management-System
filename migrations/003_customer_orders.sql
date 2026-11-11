USE restaurant_management;

-- Customer orders are placed by customers, not by a cashier, so cashier_id can be empty
ALTER TABLE orders
    MODIFY cashier_id INT NULL,
    ADD COLUMN customer_id INT NULL AFTER cashier_id,
    ADD CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES users(id);
