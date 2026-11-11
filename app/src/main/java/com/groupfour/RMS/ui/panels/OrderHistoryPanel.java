package com.groupfour.RMS.ui.panels;

import com.groupfour.RMS.model.Order;
import com.groupfour.RMS.model.OrderItem;
import com.groupfour.RMS.model.OrderStep;
import com.groupfour.RMS.model.Payment;
import com.groupfour.RMS.service.AuthService;
import com.groupfour.RMS.service.OrderService;
import com.groupfour.RMS.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
        this.orderService = new OrderService();
        initComponents();
        loadOrders();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton refreshButton = Theme.secondaryButton("Refresh");
        JButton openButton = Theme.primaryButton("Open Order");
        toolbar.add(refreshButton);
        toolbar.add(openButton);
        add(Theme.headerWithToolbar("Orders",
                "Select an order and click Open Order to see its items, progress and payment.", toolbar), BorderLayout.NORTH);

        String[] columns = {"Order ID", "Type", "Table / Address", "Cashier", "Total (₦)", "Status", "Created At"};
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
        openButton.addActionListener(e -> openOrder());
        orderTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) openOrder();
            }
        });
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
                order.getCreatedAt()
            });
        }
    }

    private void openOrder() {
        int selectedRow = orderTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int orderId = (int) tableModel.getValueAt(selectedRow, 0);
        Order order = orderService.getOrderById(orderId);
        if (order == null) return;

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Order #" + orderId, true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(540, 480);
        dialog.setLocationRelativeTo(this);

        String place = order.getOrderType() == Order.OrderType.REMOTE
                ? "Delivery: " + order.getDeliveryAddress()
                : "Table " + order.getTableNumber();
        JLabel header = new JLabel("Order #" + orderId + "  ·  " + place + "  ·  " + order.getStatus());
        header.setFont(Theme.font(Font.BOLD, 14));
        header.setForeground(Theme.TEXT);
        header.setBorder(new EmptyBorder(12, 14, 8, 14));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.BODY_BOLD);
        tabs.addTab("Items", itemsTab(order));
        tabs.addTab("Progress", progressTab(dialog, order));
        tabs.addTab("Payment", paymentTab(dialog, order));

        dialog.add(header, BorderLayout.NORTH);
        dialog.add(tabs, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private JComponent itemsTab(Order order) {
        StringBuilder text = new StringBuilder();
        for (OrderItem item : order.getOrderItems()) {
            text.append(String.format("%-28s  x%-3d  ₦%s%n", item.getItemName(), item.getQuantity(), item.getSubtotal()));
        }
        text.append("\nTotal: ₦").append(order.getTotalAmount());

        JTextArea area = new JTextArea(text.toString());
        area.setEditable(false);
        area.setFont(Theme.font(Font.PLAIN, 14));
        area.setMargin(new Insets(12, 12, 12, 12));
        return new JScrollPane(area);
    }

    private JComponent progressTab(JDialog dialog, Order order) {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel steps = new JPanel(new GridLayout(0, 1, 0, 6));
        for (OrderStep step : order.progressSteps()) {
            String state = switch (step.getState()) {
                case "done" -> "Done";
                case "current" -> "Now";
                default -> "To do";
            };
            JLabel label = new JLabel(step.getLabel() + "   —   " + state);
            label.setFont(step.getState().equals("current") ? Theme.BODY_BOLD : Theme.BODY);
            label.setForeground(step.getState().equals("todo") ? Theme.TEXT_MUTED : Theme.TEXT);
            steps.add(label);
        }
        panel.add(steps, BorderLayout.NORTH);

        String nextLabel = order.nextActionLabel();
        JButton nextButton = Theme.primaryButton(nextLabel == null ? "No more steps" : nextLabel);
        nextButton.setEnabled(nextLabel != null);
        nextButton.addActionListener(e -> {
            try {
                orderService.advanceStatus(order.getId(), authService.getCurrentUser());
                dialog.dispose();
                loadOrders();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.add(nextButton, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent paymentTab(JDialog dialog, Order order) {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        if (order.getStatus() == Order.OrderStatus.PAID || order.getStatus() == Order.OrderStatus.COMPLETED) {
            JLabel paid = new JLabel("This order has been paid in full.");
            paid.setFont(Theme.BODY_BOLD);
            paid.setForeground(Theme.SUCCESS);
            panel.add(paid, BorderLayout.NORTH);
            return panel;
        }
        if (!order.canBePaid()) {
            JLabel wait = new JLabel("Payment opens when the order is ready to pay.");
            wait.setFont(Theme.BODY);
            wait.setForeground(Theme.TEXT_MUTED);
            panel.add(wait, BorderLayout.NORTH);
            return panel;
        }

        JPanel choose = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        choose.setOpaque(false);
        JLabel question = new JLabel("How did the customer pay?");
        question.setFont(Theme.BODY);
        JComboBox<Payment.PaymentMethod> methods = new JComboBox<>(Payment.PaymentMethod.values());
        choose.add(question);
        choose.add(methods);
        panel.add(choose, BorderLayout.NORTH);

        JButton payButton = Theme.successButton("Record payment");
        payButton.addActionListener(e -> {
            try {
                if (orderService.processPayment(order.getId(), (Payment.PaymentMethod) methods.getSelectedItem(),
                        authService.getCurrentUser())) {
                    JOptionPane.showMessageDialog(dialog, "Payment recorded. Order is paid.");
                    dialog.dispose();
                    loadOrders();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Payment could not be recorded.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.add(payButton, BorderLayout.SOUTH);
        return panel;
    }
}
