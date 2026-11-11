package com.groupfour.RMS.ui.panels;

import com.groupfour.RMS.config.DBConfig;
import com.groupfour.RMS.ui.theme.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

public class ReportsPanel extends JPanel {
    private JComboBox<String> reportTypeCombo;
    private JTextField startDateField;
    private JTextField endDateField;
    private JButton generateButton;
    private JTable reportTable;
    private DefaultTableModel tableModel;
    private JLabel summaryLabel;
    
    public ReportsPanel() {
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        // Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterPanel.setBackground(Theme.BG);
        filterPanel.setBorder(BorderFactory.createTitledBorder("Report Filters"));
        
        filterPanel.add(new JLabel("Report Type:"));
        reportTypeCombo = new JComboBox<>(new String[]{"Sales Summary", "Sales by Payment Method", "Sales by Item"});
        filterPanel.add(reportTypeCombo);
        
        filterPanel.add(new JLabel("Start Date (YYYY-MM-DD):"));
        startDateField = new JTextField(LocalDate.now().minusDays(7).toString(), 10);
        filterPanel.add(startDateField);
        
        filterPanel.add(new JLabel("End Date (YYYY-MM-DD):"));
        endDateField = new JTextField(LocalDate.now().toString(), 10);
        filterPanel.add(endDateField);
        
        generateButton = Theme.primaryButton("Generate Report");
        generateButton.addActionListener(e -> generateReport());
        filterPanel.add(generateButton);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.BG);
        top.add(Theme.pageHeader("Sales Reports",
                "Filter by date range and choose a breakdown."), BorderLayout.NORTH);
        top.add(filterPanel, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel();
        reportTable = new JTable(tableModel);
        Theme.styleTable(reportTable);
        JScrollPane scrollPane = new JScrollPane(reportTable);
        Theme.styleScrollPane(scrollPane);
        add(scrollPane, BorderLayout.CENTER);

        // Summary
        summaryLabel = new JLabel(" ");
        summaryLabel.setFont(Theme.BODY_BOLD);
        summaryLabel.setForeground(Theme.PRIMARY);
        summaryLabel.setOpaque(true);
        summaryLabel.setBackground(Theme.SURFACE);
        summaryLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        add(summaryLabel, BorderLayout.SOUTH);
    }
    
    private void generateReport() {
        String reportType = (String) reportTypeCombo.getSelectedItem();
        String startDate = startDateField.getText().trim();
        String endDate = endDateField.getText().trim();
        
        try {
            LocalDate.parse(startDate);
            LocalDate.parse(endDate);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        switch (reportType) {
            case "Sales Summary" -> generateSalesSummary(startDate, endDate);
            case "Sales by Payment Method" -> generateSalesByPaymentMethod(startDate, endDate);
            case "Sales by Item" -> generateSalesByItem(startDate, endDate);
        }
    }
    
    private void generateSalesSummary(String startDate, String endDate) {
        String sql = "SELECT DATE(o.created_at) as sale_date, COUNT(DISTINCT o.id) as order_count, " +
                     "SUM(o.total_amount) as total_sales " +
                     "FROM orders o " +
                     "WHERE o.status = 'PAID' AND DATE(o.created_at) BETWEEN ? AND ? " +
                     "GROUP BY DATE(o.created_at) ORDER BY sale_date";
        
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, startDate);
            stmt.setString(2, endDate);
            ResultSet rs = stmt.executeQuery();
            
            tableModel.setColumnIdentifiers(new String[]{"Date", "Order Count", "Total Sales (₦)"});
            tableModel.setRowCount(0);
            
            BigDecimal grandTotal = BigDecimal.ZERO;
            int totalOrders = 0;
            
            while (rs.next()) {
                BigDecimal total = rs.getBigDecimal("total_sales");
                int count = rs.getInt("order_count");
                tableModel.addRow(new Object[]{
                    rs.getDate("sale_date"),
                    count,
                    total
                });
                grandTotal = grandTotal.add(total);
                totalOrders += count;
            }
            
            summaryLabel.setText(String.format("Total Orders: %d | Grand Total: ₦%s", totalOrders, grandTotal));
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error generating report: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void generateSalesByPaymentMethod(String startDate, String endDate) {
        String sql = "SELECT p.payment_method, COUNT(*) as payment_count, SUM(p.amount) as total_amount " +
                     "FROM payments p JOIN orders o ON p.order_id = o.id " +
                     "WHERE p.status = 'SUCCESSFUL' AND o.status = 'PAID' AND DATE(p.created_at) BETWEEN ? AND ? " +
                     "GROUP BY p.payment_method";
        
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, startDate);
            stmt.setString(2, endDate);
            ResultSet rs = stmt.executeQuery();
            
            tableModel.setColumnIdentifiers(new String[]{"Payment Method", "Count", "Total Amount (₦)"});
            tableModel.setRowCount(0);
            
            BigDecimal grandTotal = BigDecimal.ZERO;
            
            while (rs.next()) {
                BigDecimal total = rs.getBigDecimal("total_amount");
                tableModel.addRow(new Object[]{
                    rs.getString("payment_method"),
                    rs.getInt("payment_count"),
                    total
                });
                grandTotal = grandTotal.add(total);
            }
            
            summaryLabel.setText("Grand Total: ₦" + grandTotal);
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error generating report: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void generateSalesByItem(String startDate, String endDate) {
        String sql = "SELECT oi.item_name, SUM(oi.quantity) as total_quantity, SUM(oi.subtotal) as total_sales " +
                     "FROM order_items oi JOIN orders o ON oi.order_id = o.id " +
                     "WHERE o.status = 'PAID' AND DATE(o.created_at) BETWEEN ? AND ? " +
                     "GROUP BY oi.item_name ORDER BY total_sales DESC";
        
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, startDate);
            stmt.setString(2, endDate);
            ResultSet rs = stmt.executeQuery();
            
            tableModel.setColumnIdentifiers(new String[]{"Item Name", "Quantity Sold", "Total Sales (₦)"});
            tableModel.setRowCount(0);
            
            BigDecimal grandTotal = BigDecimal.ZERO;
            
            while (rs.next()) {
                BigDecimal total = rs.getBigDecimal("total_sales");
                tableModel.addRow(new Object[]{
                    rs.getString("item_name"),
                    rs.getInt("total_quantity"),
                    total
                });
                grandTotal = grandTotal.add(total);
            }
            
            summaryLabel.setText("Grand Total: ₦" + grandTotal);
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error generating report: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
