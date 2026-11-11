package com.groupfour.RMS.ui.panels;

import com.groupfour.RMS.model.Order;
import com.groupfour.RMS.model.OrderItem;
import com.groupfour.RMS.service.AuthService;
import com.groupfour.RMS.service.OrderService;
import com.groupfour.RMS.ui.theme.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class KitchenPanel extends JPanel {
    private AuthService authService;
    private OrderService orderService;
    private JTable orderTable;
    private DefaultTableModel tableModel;

    public KitchenPanel(AuthService authService) {
        this.authService = authService;
        this.orderService = new OrderService();
        initComponents();
        loadOrders();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton refreshButton = Theme.secondaryButton("Refresh");
        JButton readyButton = Theme.successButton("Mark Ready");
        toolbar.add(refreshButton);
        toolbar.add(readyButton);
        add(Theme.headerWithToolbar("Kitchen",
                "Orders waiting to be cooked, oldest first. Mark each one ready when it leaves the kitchen.", toolbar), BorderLayout.NORTH);

        String[] columns = {"Order ID", "Type", "Table / Address", "Items", "Placed At"};
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
        readyButton.addActionListener(e -> markReady());
    }

    private void loadOrders() {
        tableModel.setRowCount(0);
        List<Order> orders = orderService.getKitchenQueue();
        for (Order order : orders) {
            tableModel.addRow(new Object[]{
                order.getId(),
                order.getOrderType(),
                order.getOrderType() == Order.OrderType.REMOTE ? order.getDeliveryAddress() : order.getTableNumber(),
                itemsText(order),
                order.getCreatedAt()
            });
        }
    }

    private String itemsText(Order order) {
        StringBuilder text = new StringBuilder();
        for (OrderItem item : order.getOrderItems()) {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(item.getQuantity()).append(" x ").append(item.getItemName());
        }
        return text.toString();
    }

    private void markReady() {
        int selectedRow = orderTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int orderId = (int) tableModel.getValueAt(selectedRow, 0);
        try {
            orderService.advanceStatus(orderId, authService.getCurrentUser());
            loadOrders();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
