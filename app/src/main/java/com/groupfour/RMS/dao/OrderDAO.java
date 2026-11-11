package com.groupfour.RMS.dao;

import com.groupfour.RMS.config.DBConfig;
import com.groupfour.RMS.model.Order;
import com.groupfour.RMS.model.OrderItem;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {
    
    public int createOrder(Order order) {
        String sql = "INSERT INTO orders (cashier_id, total_amount, status, order_type, table_number, delivery_address) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int orderId = -1;
        
        try {
            conn = DBConfig.getConnection();
            conn.setAutoCommit(false);
            
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, order.getCashierId());
            stmt.setBigDecimal(2, order.getTotalAmount());
            stmt.setString(3, order.getStatus().name());
            stmt.setString(4, order.getOrderType().name());
            stmt.setString(5, order.getTableNumber());
            stmt.setString(6, order.getDeliveryAddress());
            
            stmt.executeUpdate();
            rs = stmt.getGeneratedKeys();
            
            if (rs.next()) {
                orderId = rs.getInt(1);
            }
            
            // Insert order items
            if (orderId > 0 && !order.getOrderItems().isEmpty()) {
                String itemSql = "INSERT INTO order_items (order_id, menu_item_id, item_name, quantity, unit_price, subtotal) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement itemStmt = conn.prepareStatement(itemSql)) {
                    for (OrderItem item : order.getOrderItems()) {
                        itemStmt.setInt(1, orderId);
                        if (item.getMenuItemId() != null) {
                            itemStmt.setInt(2, item.getMenuItemId());
                        } else {
                            itemStmt.setNull(2, Types.INTEGER);
                        }
                        itemStmt.setString(3, item.getItemName());
                        itemStmt.setInt(4, item.getQuantity());
                        itemStmt.setBigDecimal(5, item.getUnitPrice());
                        itemStmt.setBigDecimal(6, item.getSubtotal());
                        itemStmt.addBatch();
                    }
                    itemStmt.executeBatch();
                }
            }
            
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return -1;
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return orderId;
    }
    
    public Order getOrderById(int orderId) {
        String sql = "SELECT o.*, u.username as cashier_name FROM orders o JOIN users u ON o.cashier_id = u.id WHERE o.id = ?";
        
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                order.setOrderItems(getOrderItems(orderId));
                return order;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.*, u.username as cashier_name FROM orders o JOIN users u ON o.cashier_id = u.id ORDER BY o.created_at DESC";
        
        try (Connection conn = DBConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                orders.add(mapResultSetToOrder(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }
    
    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.*, u.username as cashier_name FROM orders o JOIN users u ON o.cashier_id = u.id WHERE o.status = ? ORDER BY o.created_at ASC";

        try (Connection conn = DBConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                orders.add(mapResultSetToOrder(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    public List<OrderItem> getOrderItems(int orderId) {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT * FROM order_items WHERE order_id = ?";
        
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                OrderItem item = new OrderItem();
                item.setId(rs.getInt("id"));
                item.setOrderId(rs.getInt("order_id"));
                item.setMenuItemId((Integer) rs.getObject("menu_item_id"));
                item.setItemName(rs.getString("item_name"));
                item.setQuantity(rs.getInt("quantity"));
                item.setUnitPrice(rs.getBigDecimal("unit_price"));
                item.setSubtotal(rs.getBigDecimal("subtotal"));
                items.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }
    
    // Changes status only if the order is still in the status we expect.
    public boolean changeStatus(int orderId, Order.OrderStatus from, Order.OrderStatus to) {
        String sql = "UPDATE orders SET status = ?, completed_at = IF(? = 'PAID', NOW(), completed_at) WHERE id = ? AND status = ?";

        try (Connection conn = DBConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, to.name());
            stmt.setString(2, to.name());
            stmt.setInt(3, orderId);
            stmt.setString(4, from.name());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Order mapResultSetToOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getInt("id"));
        order.setCashierId(rs.getInt("cashier_id"));
        order.setCashierName(rs.getString("cashier_name"));
        order.setOrderType(Order.OrderType.valueOf(rs.getString("order_type")));
        order.setTableNumber(rs.getString("table_number"));
        order.setDeliveryAddress(rs.getString("delivery_address"));
        order.setTotalAmount(rs.getBigDecimal("total_amount"));
        order.setStatus(Order.OrderStatus.valueOf(rs.getString("status")));
        order.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp completedAt = rs.getTimestamp("completed_at");
        if (completedAt != null) {
            order.setCompletedAt(completedAt.toLocalDateTime());
        }
        return order;
    }
}
