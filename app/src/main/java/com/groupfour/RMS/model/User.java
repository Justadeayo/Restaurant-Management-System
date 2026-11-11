package com.groupfour.RMS.model;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private UserRole role;
    private boolean isActive;
    private LocalDateTime createdAt;
    
    public enum UserRole {
        MANAGER, CASHIER, KITCHEN, RIDER, CUSTOMER
    }
    
    public User() {}
    
    public User(int id, String username, String passwordHash, UserRole role, boolean isActive, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public boolean isManager() {
        return role == UserRole.MANAGER;
    }
    
    public boolean isCashier() {
        return role == UserRole.CASHIER;
    }
}
