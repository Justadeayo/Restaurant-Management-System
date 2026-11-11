package com.groupfour.RMS.service;

import com.groupfour.RMS.dao.MenuItemDAO;
import com.groupfour.RMS.dao.OrderDAO;
import com.groupfour.RMS.dao.PaymentDAO;
import com.groupfour.RMS.model.*;

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
        validateDestination(order);

        for (OrderItem item : order.getOrderItems()) {
            if (item.getMenuItemId() == null) {
                throw new IllegalArgumentException("Only menu items can be ordered: " + item.getItemName());
            }
            MenuItem menuItem = menuItemDAO.getMenuItemById(item.getMenuItemId());
            if (menuItem == null || !menuItem.isAvailable()) {
                throw new IllegalArgumentException("Menu item not available: " + item.getItemName());
            }
            if (menuItem.getStockQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for: " + item.getItemName());
            }
            item.setItemName(menuItem.getName());
            item.setUnitPrice(menuItem.getPrice());
        }

        order.calculateTotal();
        return orderDAO.createOrder(order);
    }

    private void validateDestination(Order order) {
        if (order.getOrderType() == Order.OrderType.REMOTE) {
            String address = trimToNull(order.getDeliveryAddress());
            if (address == null) {
                throw new IllegalArgumentException("Enter a delivery address for a remote order");
            }
            order.setDeliveryAddress(address);
            order.setTableNumber(null);
        } else {
            String table = trimToNull(order.getTableNumber());
            if (table == null) {
                throw new IllegalArgumentException("Enter a table number for a table order");
            }
            order.setTableNumber(table);
            order.setDeliveryAddress(null);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Moves an order to its next step. The database update only succeeds if the
    // order is still in the status we read, so two staff cannot skip a step together.
    public void advanceStatus(int orderId, User user) {
        if (!user.isManager() && !user.isCashier() && !user.isKitchen()) {
            throw new IllegalArgumentException("Only staff can change order status");
        }
        Order order = orderDAO.getOrderById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }
        if (user.isKitchen() && order.getStatus() != Order.OrderStatus.SENT_TO_KITCHEN) {
            throw new IllegalArgumentException("Kitchen can only mark orders that were sent to the kitchen as ready");
        }
        Order.OrderStatus next = order.nextStatus();
        if (next == null) {
            throw new IllegalArgumentException("This order cannot move on from " + order.getStatus());
        }
        if (!orderDAO.changeStatus(orderId, order.getStatus(), next)) {
            throw new IllegalArgumentException("This order was changed by someone else. Refresh and try again.");
        }
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

        if (order.getStatus() != Order.OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Payment is recorded after the order is delivered");
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

            // Mark the order paid, only if it is still delivered
            String orderSql = "UPDATE orders SET status = 'PAID', completed_at = NOW() WHERE id = ? AND status = 'DELIVERED'";
            try (var stmt = conn.prepareStatement(orderSql)) {
                stmt.setInt(1, orderId);
                if (stmt.executeUpdate() == 0) {
                    throw new SQLException("Order is not ready for payment");
                }
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

    // Orders waiting in the kitchen, oldest first, with their items.
    public List<Order> getKitchenQueue() {
        List<Order> orders = orderDAO.getOrdersByStatus(Order.OrderStatus.SENT_TO_KITCHEN);
        for (Order order : orders) {
            order.setOrderItems(orderDAO.getOrderItems(order.getId()));
        }
        return orders;
    }

    public Order getOrderById(int orderId) {
        return orderDAO.getOrderById(orderId);
    }
}
