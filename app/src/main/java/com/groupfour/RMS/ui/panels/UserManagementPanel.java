package com.groupfour.RMS.ui.panels;

import com.groupfour.RMS.dao.UserDAO;
import com.groupfour.RMS.model.User;
import com.groupfour.RMS.service.AuthService;
import com.groupfour.RMS.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class UserManagementPanel extends JPanel {
    private AuthService authService;
    private UserDAO userDAO;
    private JPanel cardGrid;

    public UserManagementPanel(AuthService authService) {
        this.authService = authService;
        this.userDAO = new UserDAO();
        initComponents();
        loadUsers();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton addButton = Theme.primaryButton("Add User");
        JButton refreshButton = Theme.secondaryButton("Refresh");
        toolbar.add(addButton);
        toolbar.add(refreshButton);
        add(Theme.headerWithToolbar("User Management",
                "Create staff accounts and control access.", toolbar), BorderLayout.NORTH);

        cardGrid = new JPanel(new GridLayout(0, 2, 12, 12));
        cardGrid.setBackground(Theme.BG);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Theme.BG);
        wrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        wrapper.add(cardGrid, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(wrapper);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        Theme.styleScrollPane(scrollPane);
        add(scrollPane, BorderLayout.CENTER);

        addButton.addActionListener(e -> showAddUserDialog());
        refreshButton.addActionListener(e -> loadUsers());
    }

    private void loadUsers() {
        cardGrid.removeAll();
        List<User> users = userDAO.getStaffUsers();
        for (User user : users) {
            cardGrid.add(buildCard(user));
        }
        cardGrid.revalidate();
        cardGrid.repaint();
    }

    private JPanel buildCard(User user) {
        JPanel card = Theme.card(new BorderLayout(0, 12));

        JLabel name = new JLabel(user.getUsername());
        name.setFont(Theme.font(Font.BOLD, 16));
        name.setForeground(Theme.TEXT);
        JLabel role = new JLabel(user.getRole().name());
        role.setFont(Theme.BODY);
        role.setForeground(Theme.TEXT_MUTED);
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(name);
        titles.add(role);

        JLabel status = new JLabel(user.isActive() ? "Active" : "Inactive");
        status.setFont(Theme.BODY_BOLD);
        status.setForeground(user.isActive() ? Theme.SUCCESS : Theme.TEXT_MUTED);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titles, BorderLayout.WEST);
        header.add(status, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JPanel middle = new JPanel(new GridLayout(0, 1, 6, 6));
        middle.setOpaque(false);
        JLabel passwordLabel = new JLabel("Change password");
        passwordLabel.setFont(Theme.BODY);
        passwordLabel.setForeground(Theme.TEXT_MUTED);
        JPasswordField passwordField = new JPasswordField(12);
        JButton changeButton = Theme.secondaryButton("Change");
        changeButton.addActionListener(e -> changePassword(user, passwordField));
        JPanel passwordRow = new JPanel(new BorderLayout(8, 0));
        passwordRow.setOpaque(false);
        passwordRow.add(com.groupfour.RMS.ui.theme.PasswordToggle.wrap(passwordField), BorderLayout.CENTER);
        passwordRow.add(changeButton, BorderLayout.EAST);
        middle.add(passwordLabel);
        middle.add(passwordRow);
        card.add(middle, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(0, 1, 6, 6));
        actions.setOpaque(false);
        JButton toggleButton = user.isActive() ? Theme.secondaryButton("Deactivate") : Theme.successButton("Activate");
        JButton removeButton = Theme.secondaryButton("Remove access");
        JButton deleteButton = Theme.dangerButton("Delete");
        toggleButton.addActionListener(e -> toggleUserStatus(user));
        removeButton.addActionListener(e -> removeAccess(user));
        deleteButton.addActionListener(e -> deleteUser(user));
        actions.add(toggleButton);
        actions.add(removeButton);
        actions.add(deleteButton);
        card.add(actions, BorderLayout.SOUTH);

        return card;
    }

    private boolean isSelf(User user) {
        return user.getId() == authService.getCurrentUser().getId();
    }

    private void changePassword(User user, JPasswordField passwordField) {
        String password = new String(passwordField.getPassword());
        if (password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (userDAO.updatePassword(user.getId(), password)) {
            JOptionPane.showMessageDialog(this, "Password changed for " + user.getUsername() + ".");
            passwordField.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "Failed to change password", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleUserStatus(User user) {
        boolean activate = !user.isActive();
        if (!activate && isSelf(user)) {
            JOptionPane.showMessageDialog(this, "You cannot deactivate your own account.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to " + (activate ? "activate" : "deactivate") + " " + user.getUsername() + "?",
            "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        if (userDAO.updateUserStatus(user.getId(), activate)) {
            loadUsers();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update user status", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Deletes only accounts with no orders, payments or stock movements.
    private void deleteUser(User user) {
        if (isSelf(user)) {
            JOptionPane.showMessageDialog(this, "You cannot delete your own account.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete " + user.getUsername() + "? This cannot be undone.", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        if (userDAO.deleteUnusedUser(user.getId())) {
            JOptionPane.showMessageDialog(this, "Account deleted.");
            loadUsers();
        } else {
            JOptionPane.showMessageDialog(this,
                "This account has orders or payments, so it cannot be deleted. Use Remove access instead.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Blocks sign-in and replaces the password with a random one.
    private void removeAccess(User user) {
        if (isSelf(user)) {
            JOptionPane.showMessageDialog(this, "You cannot remove your own access.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Remove access for " + user.getUsername() + "? The account cannot sign in and its password will be replaced.",
            "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        if (userDAO.removeAccess(user.getId())) {
            JOptionPane.showMessageDialog(this, "Access removed.");
            loadUsers();
        } else {
            JOptionPane.showMessageDialog(this, "Access could not be removed.", "Error", JOptionPane.ERROR_MESSAGE);
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
        gbc.gridx = 1; gbc.gridy = row-1; dialog.add(com.groupfour.RMS.ui.theme.PasswordToggle.wrap(passwordField), gbc);
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
}
