package com.groupfour.RMS.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private int id;
    private int cashierId;
    private Integer customerId;
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
        PLACED, SENT_TO_KITCHEN, READY, OUT_FOR_DELIVERY, PAID, COMPLETED, CANCELLED
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

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

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

    // The only next step an order can take from the kitchen, rider or manager, or null when it has none.
    // Payment moves a ready table order to COMPLETED, or an out-for-delivery remote order to PAID.
    public OrderStatus nextStatus() {
        if (status == OrderStatus.PLACED) {
            return OrderStatus.SENT_TO_KITCHEN;
        }
        if (status == OrderStatus.SENT_TO_KITCHEN) {
            return OrderStatus.READY;
        }
        if (status == OrderStatus.READY && orderType == OrderType.REMOTE) {
            return OrderStatus.OUT_FOR_DELIVERY;
        }
        if (status == OrderStatus.PAID && orderType == OrderType.REMOTE) {
            return OrderStatus.COMPLETED;
        }
        return null;
    }

    // True when the order can be paid right now: a table order that is ready,
    // or a remote order whose rider has arrived.
    public boolean canBePaid() {
        return (orderType == OrderType.TABLE && status == OrderStatus.READY)
            || (orderType == OrderType.REMOTE && status == OrderStatus.OUT_FOR_DELIVERY);
    }

    // The steps this order goes through. Done steps are ticked, the current one is highlighted,
    // and every step is ticked once the order is COMPLETED.
    public List<OrderStep> progressSteps() {
        List<OrderStatus> flow = new ArrayList<>();
        flow.add(OrderStatus.PLACED);
        flow.add(OrderStatus.SENT_TO_KITCHEN);
        flow.add(OrderStatus.READY);
        if (orderType == OrderType.REMOTE) {
            flow.add(OrderStatus.OUT_FOR_DELIVERY);
        }
        flow.add(OrderStatus.PAID);
        flow.add(OrderStatus.COMPLETED);

        int current = flow.indexOf(status);
        boolean finished = status == OrderStatus.COMPLETED;
        List<OrderStep> steps = new ArrayList<>();
        for (int i = 0; i < flow.size(); i++) {
            String state = (finished || i < current) ? "done" : (i == current ? "current" : "todo");
            steps.add(new OrderStep(stepLabel(flow.get(i)), state));
        }
        return steps;
    }

    private static String stepLabel(OrderStatus step) {
        return switch (step) {
            case PLACED -> "Order placed";
            case SENT_TO_KITCHEN -> "Sent to kitchen";
            case READY -> "Ready";
            case OUT_FOR_DELIVERY -> "Out for delivery";
            case PAID -> "Paid";
            case COMPLETED -> "Completed";
            default -> step.name();
        };
    }

    // Button text for the next step, or null when there is no next step.
    public String nextActionLabel() {
        OrderStatus next = nextStatus();
        if (next == null) {
            return null;
        }
        return switch (next) {
            case SENT_TO_KITCHEN -> "Send to kitchen";
            case READY -> "Mark as ready";
            case OUT_FOR_DELIVERY -> "Mark out for delivery";
            case COMPLETED -> "Mark as delivered";
            case PAID -> "Mark as paid";
            default -> "Next step";
        };
    }
}
