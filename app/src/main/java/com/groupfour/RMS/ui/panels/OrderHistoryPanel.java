package com.groupfour.RMS.ui.panels;

import com.groupfour.RMS.model.Order;
import com.groupfour.RMS.model.OrderItem;
import com.groupfour.RMS.service.OrderService;
import com.groupfour.RMS.ui.theme.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class OrderHistoryPanel extends JPanel {
    private OrderService orderService;
    private JTable orderTable;
    private DefaultTableModel tableModel;
    
    public OrderHistoryPanel() {
        orderService = new OrderService();
        initComponents();
        loadOrders();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton refreshButton = Theme.secondaryButton("Refresh");
        JButton viewButton = Theme.primaryButton("View Details");
        toolbar.add(refreshButton);
        toolbar.add(viewButton);
        add(Theme.headerWithToolbar("Orders",
                "Browse previous orders and open their details.", toolbar), BorderLayout.NORTH);
        
        String[] columns = {"Order ID", "Cashier", "Total (₦)", "Status", "Created At", "Completed At"};
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
    }
    
    private void loadOrders() {
        tableModel.setRowCount(0);
        List<Order> orders = orderService.getAllOrders();
        for (Order order : orders) {
            tableModel.addRow(new Object[]{
                order.getId(),
                order.getCashierName(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getCompletedAt()
            });
        }
    }
    
    private void viewOrderDetails() {
        int selectedRow = orderTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int orderId = (int) tableModel.getValueAt(selectedRow, 0);
        Order order = orderService.getOrderById(orderId);
        if (order == null) return;
        
        StringBuilder details = new StringBuilder();
        details.append("Order ID: ").append(order.getId()).append("\n");
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
