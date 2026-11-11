package com.groupfour.RMS.ui.panels;

import com.groupfour.RMS.model.Order;
import com.groupfour.RMS.model.OrderItem;
import com.groupfour.RMS.model.Payment;
import com.groupfour.RMS.service.AuthService;
import com.groupfour.RMS.service.OrderService;
import com.groupfour.RMS.ui.theme.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class OrderHistoryPanel extends JPanel {
    private AuthService authService;
    private OrderService orderService;
    private JTable orderTable;
    private DefaultTableModel tableModel;

    public OrderHistoryPanel(AuthService authService) {
        this.authService = authService;
        orderService = new OrderService();
        initComponents();
        loadOrders();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton refreshButton = Theme.secondaryButton("Refresh");
        JButton viewButton = Theme.primaryButton("View Details");
        JButton advanceButton = Theme.secondaryButton("Advance Status");
        JButton payButton = Theme.successButton("Record Payment");
        toolbar.add(refreshButton);
        toolbar.add(viewButton);
        toolbar.add(advanceButton);
        toolbar.add(payButton);
        add(Theme.headerWithToolbar("Orders",
                "Move orders through each step. Record payment once an order is delivered.", toolbar), BorderLayout.NORTH);

        String[] columns = {"Order ID", "Type", "Table / Address", "Cashier", "Total (₦)", "Status", "Created At", "Completed At"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        orderTable = new JTable(tableModel);
        Theme.styleTable(orderTable);
        JScrollPane scrollPane = new JScrollPane(orderTable);
        Theme.styleScrollPane(scrollPane);
        add(scrollPane, BorderLayout.CENTER);

        refreshButton.addActionListener(e -> loadOrders());
        viewButton.addActionListener(e -> viewOrderDetails());
        advanceButton.addActionListener(e -> advanceSelected());
        payButton.addActionListener(e -> paySelected());
    }

    private void loadOrders() {
        tableModel.setRowCount(0);
        List<Order> orders = orderService.getAllOrders();
        for (Order order : orders) {
            tableModel.addRow(new Object[]{
                order.getId(),
                order.getOrderType(),
                order.getOrderType() == Order.OrderType.REMOTE ? order.getDeliveryAddress() : order.getTableNumber(),
                order.getCashierName(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getCompletedAt()
            });
        }
    }

    private int selectedOrderId() {
        int selectedRow = orderTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order", "Error", JOptionPane.ERROR_MESSAGE);
            return -1;
        }
        return (int) tableModel.getValueAt(selectedRow, 0);
    }

    private void advanceSelected() {
        int orderId = selectedOrderId();
        if (orderId == -1) return;
        try {
            orderService.advanceStatus(orderId, authService.getCurrentUser());
            loadOrders();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void paySelected() {
        int orderId = selectedOrderId();
        if (orderId == -1) return;

        Order order = orderService.getOrderById(orderId);
        if (order == null) return;
        if (order.getStatus() != Order.OrderStatus.DELIVERED) {
            JOptionPane.showMessageDialog(this,
                "Only delivered orders can be paid. This order is " + order.getStatus() + ".",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[] options = {"CASH", "CARD", "TRANSFER", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
            "Order Total: ₦" + order.getTotalAmount() + "\nSelect payment method:",
            "Payment", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
            null, options, options[0]);
        if (choice < 0 || choice > 2) return;

        try {
            if (orderService.processPayment(orderId, Payment.PaymentMethod.valueOf(options[choice]), authService.getCurrentUser())) {
                JOptionPane.showMessageDialog(this, "Payment successful! Order paid.", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadOrders();
            } else {
                JOptionPane.showMessageDialog(this, "Payment failed", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewOrderDetails() {
        int orderId = selectedOrderId();
        if (orderId == -1) return;

        Order order = orderService.getOrderById(orderId);
        if (order == null) return;

        StringBuilder details = new StringBuilder();
        details.append("Order ID: ").append(order.getId()).append("\n");
        details.append("Type: ").append(order.getOrderType()).append("\n");
        if (order.getOrderType() == Order.OrderType.REMOTE) {
            details.append("Delivery address: ").append(order.getDeliveryAddress()).append("\n");
        } else {
            details.append("Table: ").append(order.getTableNumber()).append("\n");
        }
        details.append("Cashier: ").append(order.getCashierName()).append("\n");
        details.append("Status: ").append(order.getStatus()).append("\n");
        details.append("Total: ₦").append(order.getTotalAmount()).append("\n");
        details.append("Created: ").append(order.getCreatedAt()).append("\n\n");
        details.append("Items:\n");

        for (OrderItem item : order.getOrderItems()) {
            details.append(String.format("- %s x%d @ ₦%s = ₦%s\n",
                item.getItemName(), item.getQuantity(), item.getUnitPrice(), item.getSubtotal()));
        }

        JTextArea textArea = new JTextArea(details.toString());
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));

        JOptionPane.showMessageDialog(this, scrollPane, "Order Details", JOptionPane.INFORMATION_MESSAGE);
    }
}
