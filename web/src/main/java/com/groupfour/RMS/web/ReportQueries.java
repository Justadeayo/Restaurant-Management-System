package com.groupfour.RMS.web;

import com.groupfour.RMS.config.DBConfig;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class ReportQueries {
    private ReportQueries() {
    }

    public record DailySales(LocalDate date, long orderCount, BigDecimal totalSales) {
    }

    public record PaymentSales(String method, long paymentCount, BigDecimal totalAmount) {
    }

    public record ItemSales(String itemName, long totalQuantity, BigDecimal totalSales) {
    }

    public static List<DailySales> dailySales(LocalDate from, LocalDate to) {
        String sql = "SELECT DATE(o.created_at) AS sale_date, COUNT(DISTINCT o.id) AS order_count, "
                + "SUM(o.total_amount) AS total_sales "
                + "FROM orders o "
                + "WHERE o.status = 'COMPLETED' AND DATE(o.created_at) BETWEEN ? AND ? "
                + "GROUP BY DATE(o.created_at) ORDER BY sale_date";
        List<DailySales> rows = new ArrayList<>();
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(from));
            stmt.setDate(2, java.sql.Date.valueOf(to));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new DailySales(rs.getDate("sale_date").toLocalDate(),
                            rs.getLong("order_count"), rs.getBigDecimal("total_sales")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load daily sales", e);
        }
        return rows;
    }

    public static List<PaymentSales> paymentSales(LocalDate from, LocalDate to) {
        String sql = "SELECT p.payment_method, COUNT(*) AS payment_count, SUM(p.amount) AS total_amount "
                + "FROM payments p JOIN orders o ON p.order_id = o.id "
                + "WHERE p.status = 'SUCCESSFUL' AND o.status = 'COMPLETED' AND DATE(p.created_at) BETWEEN ? AND ? "
                + "GROUP BY p.payment_method";
        List<PaymentSales> rows = new ArrayList<>();
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(from));
            stmt.setDate(2, java.sql.Date.valueOf(to));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new PaymentSales(rs.getString("payment_method"),
                            rs.getLong("payment_count"), rs.getBigDecimal("total_amount")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load payment sales", e);
        }
        return rows;
    }

    public static List<ItemSales> itemSales(LocalDate from, LocalDate to) {
        String sql = "SELECT oi.item_name, SUM(oi.quantity) AS total_quantity, SUM(oi.subtotal) AS total_sales "
                + "FROM order_items oi JOIN orders o ON oi.order_id = o.id "
                + "WHERE o.status = 'COMPLETED' AND DATE(o.created_at) BETWEEN ? AND ? "
                + "GROUP BY oi.item_name ORDER BY total_sales DESC";
        List<ItemSales> rows = new ArrayList<>();
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(from));
            stmt.setDate(2, java.sql.Date.valueOf(to));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ItemSales(rs.getString("item_name"),
                            rs.getLong("total_quantity"), rs.getBigDecimal("total_sales")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load item sales", e);
        }
        return rows;
    }

    public static long pendingOrders() {
        String sql = "SELECT COUNT(*) FROM orders WHERE status = 'PENDING'";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not count open orders", e);
        }
    }
}
