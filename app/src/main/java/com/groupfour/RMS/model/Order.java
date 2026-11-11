package com.groupfour.RMS.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private int id;
    private int cashierId;
    private String cashierName;
    private OrderType orderType = OrderType.TABLE;
    private String tableNumber;
    private String deliveryAddress;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private List<OrderItem> orderItems;

    public enum OrderType {
        TABLE, REMOTE
    }

    public enum OrderStatus {
        PLACED, SENT_TO_KITCHEN, READY, OUT_FOR_DELIVERY, DELIVERED, PAID, CANCELLED
    }

    public Order() {
        this.orderItems = new ArrayList<>();
        this.totalAmount = BigDecimal.ZERO;
        this.status = OrderStatus.PLACED;
    }

    public Order(int id, int cashierId, BigDecimal totalAmount, OrderStatus status,
                 LocalDateTime createdAt, LocalDateTime completedAt) {
        this.id = id;
        this.cashierId = cashierId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.orderItems = new ArrayList<>();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCashierId() { return cashierId; }
    public void setCashierId(int cashierId) { this.cashierId = cashierId; }

    public String getCashierName() { return cashierName; }
    public void setCashierName(String cashierName) { this.cashierName = cashierName; }

    public OrderType getOrderType() { return orderType; }
    public void setOrderType(OrderType orderType) { this.orderType = orderType; }

    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String tableNumber) { this.tableNumber = tableNumber; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }

    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        calculateTotal();
    }

    public void removeOrderItem(OrderItem item) {
        orderItems.remove(item);
        calculateTotal();
    }

    public void calculateTotal() {
        totalAmount = orderItems.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // The only next step an order can take, or null when it has none.
    // Payment is not a status step; it is recorded after the order is DELIVERED.
    public OrderStatus nextStatus() {
        if (status == OrderStatus.PLACED) {
            return OrderStatus.SENT_TO_KITCHEN;
        }
        if (status == OrderStatus.SENT_TO_KITCHEN) {
            return OrderStatus.READY;
        }
        if (status == OrderStatus.READY) {
            return orderType == OrderType.REMOTE ? OrderStatus.OUT_FOR_DELIVERY : OrderStatus.DELIVERED;
        }
        if (status == OrderStatus.OUT_FOR_DELIVERY) {
            return OrderStatus.DELIVERED;
        }
        return null;
    }
}
