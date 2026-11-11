USE restaurant_management;

ALTER TABLE users
    MODIFY role ENUM('MANAGER', 'CASHIER', 'KITCHEN', 'RIDER', 'CUSTOMER') NOT NULL;
