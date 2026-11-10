package com.groupfour.RMS.service;

import com.groupfour.RMS.dao.MenuItemDAO;
import com.groupfour.RMS.dao.OrderDAO;
import com.groupfour.RMS.dao.PaymentDAO;
import com.groupfour.RMS.model.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.groupfour.RMS.config.DBConfig;

public class OrderService {
    private OrderDAO orderDAO;
    private MenuItemDAO menuItemDAO;
    private PaymentDAO paymentDAO;
    
    public OrderService() {
        this.orderDAO = new OrderDAO();
        this.menuItemDAO = new MenuItemDAO();
        this.paymentDAO = new PaymentDAO();
    }
    
    public int createOrder(Order order, User cashier) {
        order.setCashierId(cashier.getId());
        order.calculateTotal();
        
        // Validate stock availability
        for (OrderItem item : order.getOrderItems()) {
            if (item.getMenuItemId() != null) {
                MenuItem menuItem = menuItemDAO.getMenuItemById(item.getMenuItemId());
                if (menuItem == null || !menuItem.isAvailable()) {
                    throw new IllegalArgumentException("Menu item not available: " + item.getItemName());
                }
                if (menuItem.getStockQuantity() < item.getQuantity()) {
                    throw new IllegalArgumentException("Insufficient stock for: " + item.getItemName());
                }
            }
        }
        
        return orderDAO.createOrder(order);
    }
    
    public boolean processPayment(int orderId, Payment.PaymentMethod method, User processedBy) {
        Order order = orderDAO.getOrderById(orderId);
        if (order == null) {
            return false;
        }
        
        if (paymentDAO.hasSuccessfulPayment(orderId)) {
            throw new IllegalArgumentException("Order already has a successful payment");
        }
        
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot pay for cancelled order");
        }
        
        Connection conn = null;
        try {
            conn = DBConfig.getConnection();
            conn.setAutoCommit(false);
            
            // Create payment
            Payment payment = new Payment();
            payment.setOrderId(orderId);
            payment.setAmount(order.getTotalAmount());
            payment.setPaymentMethod(method);
            payment.setStatus(Payment.PaymentStatus.SUCCESSFUL);
            payment.setProcessedBy(processedBy.getId());
            
            // Insert payment
            String paymentSql = "INSERT INTO payments (order_id, amount, payment_method, status, processed_by) VALUES (?, ?, ?, ?, ?)";
            try (var stmt = conn.prepareStatement(paymentSql)) {
                stmt.setInt(1, payment.getOrderId());
                stmt.setBigDecimal(2, payment.getAmount());
                stmt.setString(3, payment.getPaymentMethod().name());
                stmt.setString(4, payment.getStatus().name());
                stmt.setInt(5, payment.getProcessedBy());
                stmt.executeUpdate();
            }
            
            // Deduct stock
            for (OrderItem item : order.getOrderItems()) {
                if (item.getMenuItemId() != null) {
                    String stockSql = "UPDATE menu_items SET stock_quantity = stock_quantity - ? WHERE id = ? AND stock_quantity >= ?";
                    try (var stmt = conn.prepareStatement(stockSql)) {
                        stmt.setInt(1, item.getQuantity());
                        stmt.setInt(2, item.getMenuItemId());
                        stmt.setInt(3, item.getQuantity());
                        int updated = stmt.executeUpdate();
                        if (updated == 0) {
                            throw new SQLException("Insufficient stock");
                        }
                    }
                }
            }
            
            // Update order status
            String orderSql = "UPDATE orders SET status = 'COMPLETED', completed_at = NOW() WHERE id = ?";
            try (var stmt = conn.prepareStatement(orderSql)) {
                stmt.setInt(1, orderId);
                stmt.executeUpdate();
            }
            
            conn.commit();
            return true;
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public List<Order> getAllOrders() {
        return orderDAO.getAllOrders();
    }
    
    public Order getOrderById(int orderId) {
        return orderDAO.getOrderById(orderId);
    }
}
