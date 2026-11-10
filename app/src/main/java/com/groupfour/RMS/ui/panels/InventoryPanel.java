package com.groupfour.RMS.ui.panels;

import com.groupfour.RMS.dao.MenuItemDAO;
import com.groupfour.RMS.model.MenuItem;
import com.groupfour.RMS.ui.theme.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class InventoryPanel extends JPanel {
    private MenuItemDAO menuItemDAO;
    private JTable inventoryTable;
    private DefaultTableModel tableModel;
    
    public InventoryPanel() {
        menuItemDAO = new MenuItemDAO();
        initComponents();
        loadInventory();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton adjustButton = Theme.primaryButton("Adjust Stock");
        JButton refreshButton = Theme.secondaryButton("Refresh");
        toolbar.add(adjustButton);
        toolbar.add(refreshButton);
        add(Theme.headerWithToolbar("Inventory",
                "Track stock levels and low-stock warnings.", toolbar), BorderLayout.NORTH);
        
        String[] columns = {"ID", "Item Name", "Category", "Current Stock", "Low Stock Threshold", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        inventoryTable = new JTable(tableModel);
        Theme.styleTable(inventoryTable);
        JScrollPane scrollPane = new JScrollPane(inventoryTable);
        Theme.styleScrollPane(scrollPane);
        add(scrollPane, BorderLayout.CENTER);
        
        adjustButton.addActionListener(e -> adjustStock());
        refreshButton.addActionListener(e -> loadInventory());
    }
    
    private void loadInventory() {
        tableModel.setRowCount(0);
        List<MenuItem> items = menuItemDAO.getAllMenuItems();
        for (MenuItem item : items) {
            String status = item.isLowStock() ? "LOW STOCK" : "OK";
            if (item.getStockQuantity() == 0) status = "OUT OF STOCK";
            tableModel.addRow(new Object[]{
                item.getId(),
                item.getName(),
                item.getCategory(),
                item.getStockQuantity(),
                item.getLowStockThreshold(),
                status
            });
        }
    }
    
    private void adjustStock() {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int id = (int) tableModel.getValueAt(selectedRow, 0);
        String itemName = (String) tableModel.getValueAt(selectedRow, 1);
        int currentStock = (int) tableModel.getValueAt(selectedRow, 3);
        
        String[] options = {"Add Stock", "Remove Stock"};
        int choice = JOptionPane.showOptionDialog(this, "Adjust stock for: " + itemName + 
            "\nCurrent stock: " + currentStock, "Adjust Stock", 
            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        
        if (choice == -1) return;
        
        String qtyStr = JOptionPane.showInputDialog(this, "Enter quantity:", "Quantity", JOptionPane.QUESTION_MESSAGE);
        if (qtyStr == null || qtyStr.trim().isEmpty()) return;
        
        try {
            int quantity = Integer.parseInt(qtyStr.trim());
            if (quantity <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be positive", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            int change = choice == 0 ? quantity : -quantity;
            if (change < 0 && currentStock + change < 0) {
                JOptionPane.showMessageDialog(this, "Cannot reduce below 0", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (menuItemDAO.updateStock(id, change)) {
                JOptionPane.showMessageDialog(this, "Stock updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadInventory();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update stock", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid quantity", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
