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
        if (cashier.isCustomer()) {
            order.setCashierId(0);
            order.setCustomerId(cashier.getId());
            order.setOrderType(Order.OrderType.REMOTE);
        } else if (cashier.isManager() || cashier.isCashier()) {
            order.setCashierId(cashier.getId());
            order.setCustomerId(null);
        } else {
            throw new IllegalArgumentException("This account cannot place orders");
        }
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
    // Builds an order from the quantities chosen on any screen, then creates it.
    // Dishes with no quantity, or that no longer exist, are skipped.
    public int placeOrder(java.util.Map<Integer, Integer> quantityByMenuItemId, String tableNumber,
                          String deliveryAddress, Order.OrderType orderType, User user) {
        com.groupfour.RMS.dao.MenuItemDAO menuItemDAO = new com.groupfour.RMS.dao.MenuItemDAO();
        Order order = new Order();
        for (java.util.Map.Entry<Integer, Integer> entry : quantityByMenuItemId.entrySet()) {
            Integer quantity = entry.getValue();
            if (quantity == null || quantity <= 0) {
                continue;
            }
            com.groupfour.RMS.model.MenuItem item = menuItemDAO.getMenuItemById(entry.getKey());
            if (item == null) {
                continue;
            }
            order.addOrderItem(new OrderItem(item.getId(), item.getName(), quantity, item.getPrice()));
        }
        if (order.getOrderItems().isEmpty()) {
            throw new IllegalArgumentException("Add at least one item to the order.");
        }
        order.setOrderType(orderType);
        order.setTableNumber(tableNumber);
        order.setDeliveryAddress(deliveryAddress);
        return createOrder(order, user);
    }

    public void advanceStatus(int orderId, User user) {
        if (!user.isManager() && !user.isCashier() && !user.isKitchen() && !user.isRider()) {
            throw new IllegalArgumentException("Only staff can change order status");
        }
        Order order = orderDAO.getOrderById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }
        if (user.isKitchen() && order.getStatus() != Order.OrderStatus.SENT_TO_KITCHEN) {
            throw new IllegalArgumentException("Kitchen can only mark orders that were sent to the kitchen as ready");
        }
        if (user.isRider() && (order.getOrderType() != Order.OrderType.REMOTE
                || (order.getStatus() != Order.OrderStatus.READY && order.getStatus() != Order.OrderStatus.PAID))) {
            throw new IllegalArgumentException("Riders can only move remote orders that are ready or paid");
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
        if (!processedBy.isManager() && !processedBy.isCashier()) {
            throw new IllegalArgumentException("Only staff can record payments");
        }
        return recordPayment(orderId, method, processedBy.getId());
    }

    // A customer pays online once their remote order is out for delivery.
    public boolean customerPay(int orderId, User customer) {
        if (getCustomerOrder(orderId, customer) == null) {
            throw new IllegalArgumentException("Order not found");
        }
        return recordPayment(orderId, Payment.PaymentMethod.CARD, customer.getId());
    }

    private boolean recordPayment(int orderId, Payment.PaymentMethod method, int processedById) {
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

        if (!order.canBePaid()) {
            throw new IllegalArgumentException("This order cannot be paid yet");
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
            payment.setProcessedBy(processedById);

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

            // Remote orders are PAID until the rider delivers them; table orders are complete once paid
            Order.OrderStatus paidTo = order.getOrderType() == Order.OrderType.REMOTE
                ? Order.OrderStatus.PAID : Order.OrderStatus.COMPLETED;
            String orderSql = "UPDATE orders SET status = ?, completed_at = IF(? = 'COMPLETED', NOW(), completed_at) WHERE id = ? AND status = ?";
            try (var stmt = conn.prepareStatement(orderSql)) {
                stmt.setString(1, paidTo.name());
                stmt.setString(2, paidTo.name());
                stmt.setInt(3, orderId);
                stmt.setString(4, order.getStatus().name());
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

    // A customer's own orders, newest first.
    public List<Order> getCustomerOrders(User customer) {
        return orderDAO.getOrdersForCustomer(customer.getId());
    }

    // One order, only if it belongs to this customer. Returns null otherwise.
    public Order getCustomerOrder(int orderId, User customer) {
        Order order = orderDAO.getOrderById(orderId);
        if (order == null || order.getCustomerId() == null || order.getCustomerId() != customer.getId()) {
            return null;
        }
        return order;
    }

    // Remote orders waiting for a rider: ready, or already out for delivery.
    public List<Order> getDeliveryQueue() {
        return orderDAO.getDeliveryOrders();
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
