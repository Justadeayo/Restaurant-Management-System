package com.groupfour.RMS.ui;

import com.groupfour.RMS.service.AuthService;
import com.groupfour.RMS.ui.panels.*;
import com.groupfour.RMS.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainFrame extends JFrame {
    private AuthService authService;
    private JTabbedPane tabbedPane;

    public MainFrame(AuthService authService) {
        this.authService = authService;
        initComponents();
    }

    private void initComponents() {
        setTitle("Restaurant Management System - " + authService.getCurrentUser().getRole() + " Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 740);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        getContentPane().setBackground(Theme.BG);
        setLayout(new BorderLayout());

        buildMenuBar();
        add(createTopBar(), BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(Theme.BODY_BOLD);

        if (authService.isManager()) {
            tabbedPane.addTab("Dashboard", new DashboardPanel());
            tabbedPane.addTab("Menu Management", new MenuManagementPanel());
            tabbedPane.addTab("Inventory", new InventoryPanel());
            tabbedPane.addTab("Orders", new OrderHistoryPanel());
            tabbedPane.addTab("Reports", new ReportsPanel());
            tabbedPane.addTab("User Management", new UserManagementPanel());
        } else if (authService.isCashier()) {
            tabbedPane.addTab("New Order", new NewOrderPanel(authService));
            tabbedPane.addTab("Order History", new OrderHistoryPanel());
        }

        add(tabbedPane, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private void buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu accountMenu = new JMenu("Account");
        JMenuItem logoutItem = new JMenuItem("Logout");
        logoutItem.addActionListener(e -> logout());
        accountMenu.add(logoutItem);
        menuBar.add(accountMenu);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About RMS");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Restaurant Management System\nAcademic project — payments are simulated.",
                "About", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    /** Coloured app bar showing the app name and the signed-in user. */
    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.PRIMARY);
        bar.setBorder(new EmptyBorder(10, 16, 10, 16));

        JLabel title = new JLabel("\uD83C\uDF7D  Restaurant Management System");
        title.setFont(Theme.font(Font.BOLD, 17));
        title.setForeground(Theme.TEXT_ON_DARK);

        String username = authService.getCurrentUser().getUsername();
        String role = authService.getCurrentUser().getRole().name();
        JLabel user = new JLabel("Signed in: " + username + "  ·  " + role);
        user.setFont(Theme.font(Font.PLAIN, 12));
        user.setForeground(new Color(0xC9D6D1));

        bar.add(title, BorderLayout.WEST);
        bar.add(user, BorderLayout.EAST);
        return bar;
    }

    private JPanel createStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(Theme.PRIMARY_DARK);
        status.setBorder(new EmptyBorder(4, 12, 4, 12));

        JLabel statusLabel = new JLabel("Ready");
        statusLabel.setFont(Theme.font(Font.PLAIN, 11));
        statusLabel.setForeground(new Color(0xC9D6D1));

        JLabel dbLabel = new JLabel("Simulated payments — no real transactions");
        dbLabel.setFont(Theme.font(Font.PLAIN, 11));
        dbLabel.setForeground(Theme.ACCENT);

        status.add(statusLabel, BorderLayout.WEST);
        status.add(dbLabel, BorderLayout.EAST);
        return status;
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?",
                "Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            authService.logout();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }
}
