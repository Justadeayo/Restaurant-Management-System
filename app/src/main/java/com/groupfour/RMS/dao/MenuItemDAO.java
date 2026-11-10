package com.groupfour.RMS.dao;

import com.groupfour.RMS.config.DBConfig;
import com.groupfour.RMS.model.MenuItem;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MenuItemDAO {
    
    public List<MenuItem> getAllMenuItems() {
        List<MenuItem> menuItems = new ArrayList<>();
        String sql = "SELECT * FROM menu_items ORDER BY category, name";
        
        try (Connection conn = DBConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                menuItems.add(mapResultSetToMenuItem(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return menuItems;
    }
    
    public List<MenuItem> getAvailableMenuItems() {
        List<MenuItem> menuItems = new ArrayList<>();
        String sql = "SELECT * FROM menu_items WHERE available = TRUE ORDER BY category, name";
        
        try (Connection conn = DBConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                menuItems.add(mapResultSetToMenuItem(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return menuItems;
    }
    
    public boolean addMenuItem(MenuItem item) {
        String sql = "INSERT INTO menu_items (name, category, price, stock_quantity, low_stock_threshold, available) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, item.getName());
            stmt.setString(2, item.getCategory());
            stmt.setBigDecimal(3, item.getPrice());
            stmt.setInt(4, item.getStockQuantity());
            stmt.setInt(5, item.getLowStockThreshold());
            stmt.setBoolean(6, item.isAvailable());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean updateMenuItem(MenuItem item) {
        String sql = "UPDATE menu_items SET name = ?, category = ?, price = ?, stock_quantity = ?, low_stock_threshold = ?, available = ? WHERE id = ?";
        
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, item.getName());
            stmt.setString(2, item.getCategory());
            stmt.setBigDecimal(3, item.getPrice());
            stmt.setInt(4, item.getStockQuantity());
            stmt.setInt(5, item.getLowStockThreshold());
            stmt.setBoolean(6, item.isAvailable());
            stmt.setInt(7, item.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean updateStock(int menuItemId, int quantityChange) {
        String sql = "UPDATE menu_items SET stock_quantity = stock_quantity + ? WHERE id = ? AND stock_quantity + ? >= 0";
        
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, quantityChange);
            stmt.setInt(2, menuItemId);
            stmt.setInt(3, quantityChange);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public MenuItem getMenuItemById(int id) {
        String sql = "SELECT * FROM menu_items WHERE id = ?";
        
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToMenuItem(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private MenuItem mapResultSetToMenuItem(ResultSet rs) throws SQLException {
        MenuItem item = new MenuItem();
        item.setId(rs.getInt("id"));
        item.setName(rs.getString("name"));
        item.setCategory(rs.getString("category"));
        item.setPrice(rs.getBigDecimal("price"));
        item.setStockQuantity(rs.getInt("stock_quantity"));
        item.setLowStockThreshold(rs.getInt("low_stock_threshold"));
        item.setAvailable(rs.getBoolean("available"));
        item.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return item;
    }
}
