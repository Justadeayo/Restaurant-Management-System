package com.groupfour.RMS.ui.panels;

import com.groupfour.RMS.dao.MenuItemDAO;
import com.groupfour.RMS.model.MenuItem;
import com.groupfour.RMS.ui.theme.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class MenuManagementPanel extends JPanel {
    private MenuItemDAO menuItemDAO;
    private JTable menuTable;
    private DefaultTableModel tableModel;
    
    public MenuManagementPanel() {
        menuItemDAO = new MenuItemDAO();
        initComponents();
        loadMenuItems();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton addButton = Theme.primaryButton("Add Menu Item");
        JButton editButton = Theme.secondaryButton("Edit");
        JButton refreshButton = Theme.secondaryButton("Refresh");
        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(refreshButton);
        add(Theme.headerWithToolbar("Menu Management",
                "Create and maintain menu items, prices and availability.", toolbar),
                BorderLayout.NORTH);
        
        // Table
        String[] columns = {"ID", "Name", "Category", "Price (₦)", "Stock", "Low Stock Threshold", "Available"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        menuTable = new JTable(tableModel);
        Theme.styleTable(menuTable);
        JScrollPane scrollPane = new JScrollPane(menuTable);
        Theme.styleScrollPane(scrollPane);
        add(scrollPane, BorderLayout.CENTER);
        
        // Actions
        addButton.addActionListener(e -> showAddDialog());
        editButton.addActionListener(e -> showEditDialog());
        refreshButton.addActionListener(e -> loadMenuItems());
    }
    
    private void loadMenuItems() {
        tableModel.setRowCount(0);
        List<MenuItem> items = menuItemDAO.getAllMenuItems();
        for (MenuItem item : items) {
            tableModel.addRow(new Object[]{
                item.getId(),
                item.getName(),
                item.getCategory(),
                item.getPrice(),
                item.getStockQuantity(),
                item.getLowStockThreshold(),
                item.isAvailable() ? "Yes" : "No"
            });
        }
    }
    
    private void showAddDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Menu Item", true);
        dialog.setLayout(new GridBagLayout());
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JTextField nameField = new JTextField(15);
        JTextField categoryField = new JTextField(15);
        JTextField priceField = new JTextField(15);
        JTextField stockField = new JTextField(15);
        JTextField thresholdField = new JTextField(15);
        JCheckBox availableBox = new JCheckBox("Available", true);
        
        int row = 0;
        gbc.gridx = 0; gbc.gridy = row++; dialog.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.gridy = row-1; dialog.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy = row++; dialog.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1; gbc.gridy = row-1; dialog.add(categoryField, gbc);
        gbc.gridx = 0; gbc.gridy = row++; dialog.add(new JLabel("Price:"), gbc);
        gbc.gridx = 1; gbc.gridy = row-1; dialog.add(priceField, gbc);
        gbc.gridx = 0; gbc.gridy = row++; dialog.add(new JLabel("Stock Quantity:"), gbc);
        gbc.gridx = 1; gbc.gridy = row-1; dialog.add(stockField, gbc);
        gbc.gridx = 0; gbc.gridy = row++; dialog.add(new JLabel("Low Stock Threshold:"), gbc);
        gbc.gridx = 1; gbc.gridy = row-1; dialog.add(thresholdField, gbc);
        gbc.gridx = 1; gbc.gridy = row++; dialog.add(availableBox, gbc);
        
        JButton saveButton = Theme.successButton("Save");
        saveButton.addActionListener(e -> {
            try {
                MenuItem item = new MenuItem();
                item.setName(nameField.getText());
                item.setCategory(categoryField.getText());
                item.setPrice(new BigDecimal(priceField.getText()));
                item.setStockQuantity(Integer.parseInt(stockField.getText()));
                item.setLowStockThreshold(Integer.parseInt(thresholdField.getText()));
                item.setAvailable(availableBox.isSelected());
                
                if (menuItemDAO.addMenuItem(item)) {
                    JOptionPane.showMessageDialog(dialog, "Menu item added successfully!");
                    dialog.dispose();
                    loadMenuItems();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Failed to add menu item", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid input: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        gbc.gridx = 1; gbc.gridy = row++;
        dialog.add(saveButton, gbc);
        
        dialog.setVisible(true);
    }
    
    private void showEditDialog() {
        int selectedRow = menuTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a menu item to edit", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int id = (int) tableModel.getValueAt(selectedRow, 0);
        MenuItem item = menuItemDAO.getMenuItemById(id);
        if (item == null) return;
        
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Menu Item", true);
        dialog.setLayout(new GridBagLayout());
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JTextField nameField = new JTextField(item.getName(), 15);
        JTextField categoryField = new JTextField(item.getCategory(), 15);
        JTextField priceField = new JTextField(item.getPrice().toString(), 15);
        JTextField stockField = new JTextField(String.valueOf(item.getStockQuantity()), 15);
        JTextField thresholdField = new JTextField(String.valueOf(item.getLowStockThreshold()), 15);
        JCheckBox availableBox = new JCheckBox("Available", item.isAvailable());
        
        int row = 0;
        gbc.gridx = 0; gbc.gridy = row++; dialog.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.gridy = row-1; dialog.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy = row++; dialog.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1; gbc.gridy = row-1; dialog.add(categoryField, gbc);
        gbc.gridx = 0; gbc.gridy = row++; dialog.add(new JLabel("Price:"), gbc);
        gbc.gridx = 1; gbc.gridy = row-1; dialog.add(priceField, gbc);
        gbc.gridx = 0; gbc.gridy = row++; dialog.add(new JLabel("Stock Quantity:"), gbc);
        gbc.gridx = 1; gbc.gridy = row-1; dialog.add(stockField, gbc);
        gbc.gridx = 0; gbc.gridy = row++; dialog.add(new JLabel("Low Stock Threshold:"), gbc);
        gbc.gridx = 1; gbc.gridy = row-1; dialog.add(thresholdField, gbc);
        gbc.gridx = 1; gbc.gridy = row++; dialog.add(availableBox, gbc);
        
        JButton saveButton = Theme.successButton("Save");
        saveButton.addActionListener(e -> {
            try {
                item.setName(nameField.getText());
                item.setCategory(categoryField.getText());
                item.setPrice(new BigDecimal(priceField.getText()));
                item.setStockQuantity(Integer.parseInt(stockField.getText()));
                item.setLowStockThreshold(Integer.parseInt(thresholdField.getText()));
                item.setAvailable(availableBox.isSelected());
                
                if (menuItemDAO.updateMenuItem(item)) {
                    JOptionPane.showMessageDialog(dialog, "Menu item updated successfully!");
                    dialog.dispose();
                    loadMenuItems();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Failed to update menu item", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid input: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        gbc.gridx = 1; gbc.gridy = row++;
        dialog.add(saveButton, gbc);
        
        dialog.setVisible(true);
    }
}
