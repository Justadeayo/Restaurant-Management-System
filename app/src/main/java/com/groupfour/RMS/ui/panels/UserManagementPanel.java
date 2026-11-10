package com.groupfour.RMS.ui.panels;

import com.groupfour.RMS.dao.UserDAO;
import com.groupfour.RMS.model.User;
import com.groupfour.RMS.ui.theme.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class UserManagementPanel extends JPanel {
    private UserDAO userDAO;
    private JTable userTable;
    private DefaultTableModel tableModel;
    
    public UserManagementPanel() {
        userDAO = new UserDAO();
        initComponents();
        loadUsers();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton addButton = Theme.primaryButton("Add User");
        JButton toggleButton = Theme.secondaryButton("Activate/Deactivate");
        JButton refreshButton = Theme.secondaryButton("Refresh");
        toolbar.add(addButton);
        toolbar.add(toggleButton);
        toolbar.add(refreshButton);
        add(Theme.headerWithToolbar("User Management",
                "Create staff accounts and control access.", toolbar), BorderLayout.NORTH);
        
        String[] columns = {"ID", "Username", "Role", "Status", "Created At"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        userTable = new JTable(tableModel);
        Theme.styleTable(userTable);
        JScrollPane scrollPane = new JScrollPane(userTable);
        Theme.styleScrollPane(scrollPane);
        add(scrollPane, BorderLayout.CENTER);
        
        addButton.addActionListener(e -> showAddUserDialog());
        toggleButton.addActionListener(e -> toggleUserStatus());
        refreshButton.addActionListener(e -> loadUsers());
    }
    
    private void loadUsers() {
        tableModel.setRowCount(0);
        List<User> users = userDAO.getAllUsers();
        for (User user : users) {
            tableModel.addRow(new Object[]{
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.isActive() ? "Active" : "Inactive",
                user.getCreatedAt()
            });
        }
    }
    
    private void showAddUserDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add User", true);
        dialog.setLayout(new GridBagLayout());
        dialog.setSize(350, 250);
        dialog.setLocationRelativeTo(this);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JTextField usernameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);
        JComboBox<User.UserRole> roleCombo = new JComboBox<>(User.UserRole.values());
        
        int row = 0;
        gbc.gridx = 0; gbc.gridy = row++; dialog.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.gridy = row-1; dialog.add(usernameField, gbc);
        gbc.gridx = 0; gbc.gridy = row++; dialog.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.gridy = row-1; dialog.add(passwordField, gbc);
        gbc.gridx = 0; gbc.gridy = row++; dialog.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1; gbc.gridy = row-1; dialog.add(roleCombo, gbc);
        
        JButton saveButton = Theme.successButton("Save");
        saveButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            User.UserRole role = (User.UserRole) roleCombo.getSelectedItem();
            
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill all fields", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (userDAO.createUser(username, password, role)) {
                JOptionPane.showMessageDialog(dialog, "User created successfully!");
                dialog.dispose();
                loadUsers();
            } else {
                JOptionPane.showMessageDialog(dialog, "Failed to create user", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        gbc.gridx = 1; gbc.gridy = row++;
        dialog.add(saveButton, gbc);
        
        dialog.setVisible(true);
    }
    
    private void toggleUserStatus() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int id = (int) tableModel.getValueAt(selectedRow, 0);
        String username = (String) tableModel.getValueAt(selectedRow, 1);
        String status = (String) tableModel.getValueAt(selectedRow, 3);
        boolean isActive = status.equals("Active");
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to " + (isActive ? "deactivate" : "activate") + " " + username + "?", 
            "Confirm", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            if (userDAO.updateUserStatus(id, !isActive)) {
                JOptionPane.showMessageDialog(this, "User status updated successfully!");
                loadUsers();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update user status", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
