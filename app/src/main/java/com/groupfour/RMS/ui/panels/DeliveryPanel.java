package com.groupfour.RMS.ui.panels;

import com.groupfour.RMS.model.Order;
import com.groupfour.RMS.service.AuthService;
import com.groupfour.RMS.service.OrderService;
import com.groupfour.RMS.ui.theme.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class DeliveryPanel extends JPanel {
    private AuthService authService;
    private OrderService orderService;
    private JTable orderTable;
    private DefaultTableModel tableModel;

    public DeliveryPanel(AuthService authService) {
        this.authService = authService;
        this.orderService = new OrderService();
        initComponents();
        loadOrders();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton refreshButton = Theme.secondaryButton("Refresh");
        JButton nextButton = Theme.successButton("Mark Next Step");
        toolbar.add(refreshButton);
        toolbar.add(nextButton);
        add(Theme.headerWithToolbar("Deliveries",
                "Ready remote orders, with the address to deliver to. Select an order and move it on.", toolbar), BorderLayout.NORTH);

        String[] columns = {"Order ID", "Delivery Address", "Status", "Placed At"};
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
        nextButton.addActionListener(e -> markNextStep());
    }

    private void loadOrders() {
        tableModel.setRowCount(0);
        List<Order> orders = orderService.getDeliveryQueue();
        for (Order order : orders) {
            tableModel.addRow(new Object[]{
                order.getId(),
                order.getDeliveryAddress(),
                order.getStatus(),
                order.getCreatedAt()
            });
        }
    }

    private void markNextStep() {
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
