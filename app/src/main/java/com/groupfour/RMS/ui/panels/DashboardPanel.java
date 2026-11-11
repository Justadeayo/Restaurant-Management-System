package com.groupfour.RMS.ui.panels;

import com.groupfour.RMS.config.DBConfig;
import com.groupfour.RMS.dao.MenuItemDAO;
import com.groupfour.RMS.model.MenuItem;
import com.groupfour.RMS.ui.theme.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class DashboardPanel extends JPanel {
    private final MenuItemDAO menuItemDAO = new MenuItemDAO();
    private final JLabel salesValue = new JLabel("0.00");
    private final JLabel paidValue = new JLabel("0");
    private final JLabel openValue = new JLabel("0");
    private final JLabel lowValue = new JLabel("0");
    private DefaultTableModel lowStockModel;

    public DashboardPanel() {
        initComponents();
        loadStats();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JButton refreshButton = Theme.secondaryButton("Refresh");
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.add(refreshButton);
        add(Theme.headerWithToolbar("Dashboard", "Today at a glance.", toolbar), BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
        cards.setBackground(Theme.BG);
        cards.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        cards.add(statCard("Sales today (₦)", salesValue));
        cards.add(statCard("Paid orders today", paidValue));
        cards.add(statCard("Open (unpaid) orders", openValue));
        cards.add(statCard("Low-stock items", lowValue));

        lowStockModel = new DefaultTableModel(new String[]{"Item", "Stock", "Low at"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable lowTable = new JTable(lowStockModel);
        Theme.styleTable(lowTable);
        JScrollPane scrollPane = new JScrollPane(lowTable);
        Theme.styleScrollPane(scrollPane);

        JLabel lowTitle = new JLabel("Low stock");
        lowTitle.setFont(Theme.HEADING);
        lowTitle.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JPanel lowSection = new JPanel(new BorderLayout());
        lowSection.setBackground(Theme.BG);
        lowSection.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
        lowSection.add(lowTitle, BorderLayout.NORTH);
        lowSection.add(scrollPane, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Theme.BG);
        body.add(cards, BorderLayout.NORTH);
        body.add(lowSection, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        refreshButton.addActionListener(e -> loadStats());
    }

    private JPanel statCard(String label, JLabel value) {
        JPanel card = Theme.card(new BorderLayout());
        JLabel title = new JLabel(label);
        title.setFont(Theme.SUBTITLE);
        title.setForeground(Theme.TEXT_MUTED);
        value.setFont(Theme.TITLE);
        value.setForeground(Theme.TEXT);
        card.add(title, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private void loadStats() {
        try (Connection conn = DBConfig.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COALESCE(SUM(total_amount), 0), COUNT(*) FROM orders "
                    + "WHERE status IN ('PAID', 'COMPLETED') AND DATE(created_at) = ?")) {
                stmt.setDate(1, Date.valueOf(LocalDate.now()));
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    salesValue.setText(String.format("%,.2f", rs.getBigDecimal(1)));
                    paidValue.setText(String.valueOf(rs.getLong(2)));
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM orders WHERE status NOT IN ('COMPLETED', 'CANCELLED')");
                 ResultSet rs = stmt.executeQuery()) {
                rs.next();
                openValue.setText(String.valueOf(rs.getLong(1)));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Could not load the dashboard: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }

        List<MenuItem> lowStock = menuItemDAO.getAllMenuItems().stream()
                .filter(MenuItem::isLowStock)
                .toList();
        lowValue.setText(String.valueOf(lowStock.size()));
        lowStockModel.setRowCount(0);
        for (MenuItem item : lowStock) {
            lowStockModel.addRow(new Object[]{item.getName(), item.getStockQuantity(), item.getLowStockThreshold()});
        }
    }
}
