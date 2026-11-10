package com.groupfour.RMS.ui;

import com.groupfour.RMS.service.AuthService;
import com.groupfour.RMS.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private AuthService authService;

    public LoginFrame() {
        authService = new AuthService();
        initComponents();
    }

    private void initComponents() {
        setTitle("Restaurant Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(720, 440);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(createBrandPanel(), BorderLayout.WEST);
        root.add(createFormPanel(), BorderLayout.CENTER);

        setContentPane(root);
        getRootPane().setDefaultButton(loginButton);
        SwingUtilities.invokeLater(usernameField::requestFocusInWindow);
    }

    /** Left-hand green branding panel. */
    private JPanel createBrandPanel() {
        JPanel brand = new JPanel(new GridBagLayout());
        brand.setBackground(Theme.PRIMARY);
        brand.setPreferredSize(new Dimension(300, 0));
        brand.setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel("\uD83C\uDF7D"); // fork & knife
        logo.setFont(Theme.font(Font.PLAIN, 54));
        logo.setForeground(Theme.ACCENT);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel name = new JLabel("Restaurant");
        name.setFont(Theme.font(Font.BOLD, 26));
        name.setForeground(Theme.TEXT_ON_DARK);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel name2 = new JLabel("Management System");
        name2.setFont(Theme.font(Font.PLAIN, 16));
        name2.setForeground(new Color(0xC9D6D1));
        name2.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tagline = new JLabel("<html>Orders, payments, stock and<br>sales — all in one place.</html>");
        tagline.setFont(Theme.SUBTITLE);
        tagline.setForeground(new Color(0xA9BFB8));
        tagline.setBorder(new EmptyBorder(18, 0, 0, 0));
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(logo);
        content.add(Box.createVerticalStrut(8));
        content.add(name);
        content.add(name2);
        content.add(tagline);
        brand.add(content);
        return brand;
    }

    /** Right-hand login form. */
    private JPanel createFormPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Theme.BG);
        wrapper.setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel card = Theme.card(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridwidth = 2;

        JLabel title = new JLabel("Sign in");
        title.setFont(Theme.font(Font.BOLD, 22));
        title.setForeground(Theme.PRIMARY);
        gbc.gridx = 0; gbc.gridy = 0;
        card.add(title, gbc);

        JLabel subtitle = new JLabel("Use your staff account to continue.");
        subtitle.setFont(Theme.SUBTITLE);
        subtitle.setForeground(Theme.TEXT_MUTED);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 6, 14, 6);
        card.add(subtitle, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(6, 6, 6, 6);

        gbc.gridx = 0; gbc.gridy = 2;
        card.add(fieldLabel("Username"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(16);
        usernameField.putClientProperty("JTextField.placeholderText", "e.g. admin");
        card.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        card.add(fieldLabel("Password"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(16);
        card.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 6, 6, 6);
        loginButton = Theme.primaryButton("Sign In");
        loginButton.setPreferredSize(new Dimension(0, 38));
        card.add(loginButton, gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(4, 6, 0, 6);
        JLabel hint = new JLabel("Default account: admin / admin123", SwingConstants.CENTER);
        hint.setFont(Theme.font(Font.PLAIN, 11));
        hint.setForeground(Theme.TEXT_MUTED);
        card.add(hint, gbc);

        loginButton.addActionListener(e -> performLogin());
        passwordField.addActionListener(e -> performLogin());
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());

        wrapper.add(card);
        return wrapper;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.BODY_BOLD);
        label.setForeground(Theme.TEXT);
        return label;
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both username and password",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        loginButton.setEnabled(false);
        try {
            if (authService.login(username, password)) {
                dispose();
                new MainFrame(authService).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password",
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
                passwordField.requestFocusInWindow();
            }
        } finally {
            loginButton.setEnabled(true);
        }
    }
}
