package com.groupfour.RMS.ui.panels;

import com.groupfour.RMS.dao.MenuItemDAO;
import com.groupfour.RMS.model.MenuItem;
import com.groupfour.RMS.model.Order;
import com.groupfour.RMS.model.OrderItem;
import com.groupfour.RMS.model.Payment;
import com.groupfour.RMS.service.AuthService;
import com.groupfour.RMS.service.OrderService;
import com.groupfour.RMS.ui.theme.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.util.List;

public class NewOrderPanel extends JPanel {
    private AuthService authService;
    private OrderService orderService;
    private MenuItemDAO menuItemDAO;
    
    private JTable menuTable;
    private DefaultTableModel menuTableModel;
    private JTable orderTable;
    private DefaultTableModel orderTableModel;
    private JLabel totalLabel;
    private Order currentOrder;
    
    public NewOrderPanel(AuthService authService) {
        this.authService = authService;
        this.orderService = new OrderService();
        this.menuItemDAO = new MenuItemDAO();
        this.currentOrder = new Order();
        initComponents();
        loadMenuItems();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(500);
        
        // Left panel - Menu
        JPanel menuPanel = new JPanel(new BorderLayout());
        menuPanel.setBorder(BorderFactory.createTitledBorder("Menu Items"));
        String[] menuCols = {"ID", "Name", "Category", "Price", "Stock"};
        menuTableModel = new DefaultTableModel(menuCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        menuTable = new JTable(menuTableModel);
        Theme.styleTable(menuTable);
        JScrollPane menuScroll = new JScrollPane(menuTable);
        Theme.styleScrollPane(menuScroll);
        menuPanel.add(menuScroll, BorderLayout.CENTER);

        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        addPanel.setBackground(Theme.BG);
        JButton addItemButton = Theme.primaryButton("Add to Order");
        addItemButton.addActionListener(e -> addToOrder());
        addPanel.add(addItemButton);
        menuPanel.add(addPanel, BorderLayout.SOUTH);
        
        // Right panel - Order
        JPanel orderPanel = new JPanel(new BorderLayout());
        orderPanel.setBorder(BorderFactory.createTitledBorder("Current Order"));
        String[] orderCols = {"MenuItemId", "Item", "Price", "Quantity", "Subtotal"};
        orderTableModel = new DefaultTableModel(orderCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        orderTable = new JTable(orderTableModel);
        Theme.styleTable(orderTable);
        JScrollPane orderScroll = new JScrollPane(orderTable);
        Theme.styleScrollPane(orderScroll);
        orderPanel.add(orderScroll, BorderLayout.CENTER);
        
        JPanel orderBottomPanel = new JPanel(new BorderLayout());
        totalLabel = new JLabel("Total: ₦0.00");
        totalLabel.setFont(Theme.font(Font.BOLD, 17));
        totalLabel.setForeground(Theme.PRIMARY);
        totalLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        orderBottomPanel.add(totalLabel, BorderLayout.WEST);

        JPanel orderActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        orderActions.setBackground(Theme.BG);
        JButton removeButton = Theme.secondaryButton("Remove Item");
        JButton clearButton = Theme.secondaryButton("Clear Order");
        JButton confirmButton = Theme.successButton("Confirm Order");
        removeButton.addActionListener(e -> removeItem());
        clearButton.addActionListener(e -> clearOrder());
        confirmButton.addActionListener(e -> confirmOrder());
        orderActions.add(removeButton);
        orderActions.add(clearButton);
        orderActions.add(confirmButton);
        orderBottomPanel.add(orderActions, BorderLayout.EAST);
        
        orderPanel.add(orderBottomPanel, BorderLayout.SOUTH);
        
        splitPane.setLeftComponent(menuPanel);
        splitPane.setRightComponent(orderPanel);
        add(Theme.pageHeader("New Order",
                "Select available items, review the total, then confirm."), BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }
    
    private void loadMenuItems() {
        menuTableModel.setRowCount(0);
        List<MenuItem> items = menuItemDAO.getAvailableMenuItems();
        for (MenuItem item : items) {
            if (item.getStockQuantity() > 0) {
                menuTableModel.addRow(new Object[]{
                    item.getId(),
                    item.getName(),
                    item.getCategory(),
                    item.getPrice(),
                    item.getStockQuantity()
                });
            }
        }
    }
    
    private void addToOrder() {
        int selectedRow = menuTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a menu item", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int menuItemId = (int) menuTableModel.getValueAt(selectedRow, 0);
        String itemName = (String) menuTableModel.getValueAt(selectedRow, 1);
        BigDecimal price = (BigDecimal) menuTableModel.getValueAt(selectedRow, 3);
        int availableStock = (int) menuTableModel.getValueAt(selectedRow, 4);
        
        String qtyStr = JOptionPane.showInputDialog(this, "Enter quantity:", "Quantity", JOptionPane.QUESTION_MESSAGE);
        if (qtyStr == null || qtyStr.trim().isEmpty()) return;
        
        try {
            int quantity = Integer.parseInt(qtyStr.trim());
            if (quantity <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be positive", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (quantity > availableStock) {
                JOptionPane.showMessageDialog(this, "Not enough stock. Available: " + availableStock, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            OrderItem orderItem = new OrderItem(menuItemId, itemName, quantity, price);
            currentOrder.addOrderItem(orderItem);
            updateOrderDisplay();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid quantity", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateOrderDisplay() {
        orderTableModel.setRowCount(0);
        for (OrderItem item : currentOrder.getOrderItems()) {
            orderTableModel.addRow(new Object[]{
                item.getMenuItemId(),
                item.getItemName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal()
            });
        }
        totalLabel.setText("Total: ₦" + currentOrder.getTotalAmount());
    }
    
    private void removeItem() {
        int selectedRow = orderTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to remove", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        currentOrder.getOrderItems().remove(selectedRow);
        currentOrder.calculateTotal();
        updateOrderDisplay();
    }
    
    private void clearOrder() {
        currentOrder = new Order();
        updateOrderDisplay();
    }
    
    private void confirmOrder() {
        if (currentOrder.getOrderItems().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Order is empty", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            int orderId = orderService.createOrder(currentOrder, authService.getCurrentUser());
            if (orderId > 0) {
                JOptionPane.showMessageDialog(this, "Order created successfully! Order ID: " + orderId, 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                // Show payment dialog
                showPaymentDialog(orderId);
                clearOrder();
                loadMenuItems();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to create order", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showPaymentDialog(int orderId) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) return;
        
        String[] options = {"CASH", "CARD", "TRANSFER", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, 
            "Order Total: ₦" + order.getTotalAmount() + "\nSelect payment method:", 
            "Payment", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, 
            null, options, options[0]);
        
        if (choice >= 0 && choice < 3) {
            String method = options[choice];
            try {
                if (orderService.processPayment(orderId, Payment.PaymentMethod.valueOf(method), authService.getCurrentUser())) {
                    JOptionPane.showMessageDialog(this, "Payment successful! Order completed.", 
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Payment failed", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
