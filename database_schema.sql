-- Restaurant Management System Database Schema
-- Based on PRD v1.0

CREATE DATABASE IF NOT EXISTS restaurant_management;
USE restaurant_management;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('MANAGER', 'CASHIER', 'KITCHEN', 'RIDER', 'CUSTOMER') NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Menu items table
CREATE TABLE IF NOT EXISTS menu_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT DEFAULT 0,
    low_stock_threshold INT DEFAULT 10,
    available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_price CHECK (price >= 0),
    CONSTRAINT chk_stock CHECK (stock_quantity >= 0)
);

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cashier_id INT NOT NULL,
    total_amount DECIMAL(10, 2) DEFAULT 0.00,
    order_type ENUM('TABLE', 'REMOTE') NOT NULL DEFAULT 'TABLE',
    table_number VARCHAR(20) NULL,
    delivery_address VARCHAR(255) NULL,
    status ENUM('PLACED', 'SENT_TO_KITCHEN', 'READY', 'OUT_FOR_DELIVERY', 'DELIVERED', 'PAID', 'CANCELLED') NOT NULL DEFAULT 'PLACED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    FOREIGN KEY (cashier_id) REFERENCES users(id)
);

-- Order items table
CREATE TABLE IF NOT EXISTS order_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    menu_item_id INT NULL,
    item_name VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE SET NULL,
    CONSTRAINT chk_quantity CHECK (quantity > 0)
);

-- Payments table
CREATE TABLE IF NOT EXISTS payments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method ENUM('CASH', 'CARD', 'TRANSFER') NOT NULL,
    status ENUM('PENDING', 'SUCCESSFUL', 'FAILED', 'CANCELLED') DEFAULT 'PENDING',
    processed_by INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (processed_by) REFERENCES users(id)
);

-- Inventory movements table (optional but useful)
CREATE TABLE IF NOT EXISTS inventory_movements (
    id INT AUTO_INCREMENT PRIMARY KEY,
    menu_item_id INT NOT NULL,
    quantity_change INT NOT NULL,
    reason ENUM('SALE', 'RESTOCK', 'CORRECTION', 'CANCELLATION') NOT NULL,
    user_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create default admin user (password: admin123 - CHANGE THIS)
-- Password hash for 'admin123' using BCrypt
INSERT INTO users (username, password_hash, role, is_active) 
VALUES ('admin', '$2a$12$QwAzxQY8DHlVpyt6COw9K.phPK7Q3hZfRu6x3TuSsfwy14Bf4gtPe', 'MANAGER', TRUE)
ON DUPLICATE KEY UPDATE username=username;
