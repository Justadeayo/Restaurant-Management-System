package com.groupfour.RMS.ui;

import com.groupfour.RMS.dao.UserDAO;
import com.groupfour.RMS.service.AuthService;
import com.groupfour.RMS.ui.panels.*;
import com.groupfour.RMS.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;

public class MainFrame extends JFrame {
    private AuthService authService;
    private Timer accessTimer;

    private CardLayout cards;
    private JPanel contentPanel;
    private JLabel screenLabel;
    private JButton menuButton;
    private ArrayList<String> screens = new ArrayList<>();
    private String currentScreen;

    public MainFrame(AuthService authService) {
        this.authService = authService;
        initComponents();
        accessTimer = new Timer(30000, e -> checkAccess());
        accessTimer.start();
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

        cards = new CardLayout();
        contentPanel = new JPanel(cards);
        contentPanel.setBackground(Theme.BG);

        if (authService.isManager()) {
            addScreen("Dashboard", new DashboardPanel());
            addScreen("Menu Management", new MenuManagementPanel());
            addScreen("Inventory", new InventoryPanel());
            addScreen("Orders", new OrderHistoryPanel(authService));
            addScreen("Reports", new ReportsPanel());
            addScreen("User Management", new UserManagementPanel(authService));
        } else if (authService.isCashier()) {
            addScreen("New Order", new NewOrderPanel(authService));
            addScreen("Order History", new OrderHistoryPanel(authService));
        } else if (authService.isKitchen()) {
            addScreen("Kitchen", new KitchenPanel(authService));
        } else if (authService.isRider()) {
            addScreen("Deliveries", new DeliveryPanel(authService));
        } else if (authService.isCustomer()) {
            addScreen("Order Online", new CustomerShopPanel(authService));
            addScreen("My Orders", new MyOrdersPanel(authService));
        }

        add(contentPanel, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        if (!screens.isEmpty()) {
            showScreen(screens.get(0));
        }
    }

    private void addScreen(String name, JPanel panel) {
        screens.add(name);
        contentPanel.add(panel, name);
    }

    private void showScreen(String name) {
        currentScreen = name;
        cards.show(contentPanel, name);
        screenLabel.setText(name);
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

    /** App bar: the current screen's name on the left, the user and a Menu dropdown on the right. */
    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.PRIMARY);
        bar.setBorder(new EmptyBorder(10, 16, 10, 16));

        JLabel caption = new JLabel("🍽  RESTAURANT MANAGEMENT");
        caption.setFont(Theme.font(Font.BOLD, 10));
        caption.setForeground(new Color(0xC9D6D1));

        screenLabel = new JLabel("");
        screenLabel.setFont(Theme.font(Font.BOLD, 18));
        screenLabel.setForeground(Theme.TEXT_ON_DARK);

        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(caption);
        titles.add(screenLabel);

        String username = authService.getCurrentUser().getUsername();
        String role = authService.getCurrentUser().getRole().name();
        JLabel user = new JLabel(username + "  ·  " + role);
        user.setFont(Theme.font(Font.PLAIN, 12));
        user.setForeground(new Color(0xC9D6D1));

        menuButton = Theme.secondaryButton("Menu  ▾");
        menuButton.setFont(Theme.font(Font.BOLD, 13));
        menuButton.setBorder(new EmptyBorder(8, 16, 8, 16));
        menuButton.addActionListener(e -> buildMenu().show(menuButton, 0, menuButton.getHeight() + 4));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);
        right.add(user);
        right.add(menuButton);

        bar.add(titles, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPopupMenu buildMenu() {
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(Theme.SURFACE);
        popup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(6, 0, 6, 0)));

        JLabel header = new JLabel("  Signed in as " + authService.getCurrentUser().getUsername());
        header.setFont(Theme.font(Font.PLAIN, 12));
        header.setForeground(Theme.TEXT_MUTED);
        header.setBorder(new EmptyBorder(6, 12, 6, 24));
        popup.add(header);
        popup.addSeparator();

        for (String name : screens) {
            boolean active = name.equals(currentScreen);
            JMenuItem item = new JMenuItem((active ? "✓  " : "    ") + name);
            item.setFont(Theme.font(active ? Font.BOLD : Font.PLAIN, 14));
            item.setForeground(active ? Theme.PRIMARY : Theme.TEXT);
            item.setBackground(Theme.SURFACE);
            item.setBorder(new EmptyBorder(9, 12, 9, 36));
            item.addActionListener(e -> showScreen(name));
            popup.add(item);
        }

        popup.addSeparator();
        JMenuItem logoutItem = new JMenuItem("    Log out");
        logoutItem.setFont(Theme.font(Font.PLAIN, 14));
        logoutItem.setForeground(Theme.DANGER);
        logoutItem.setBackground(Theme.SURFACE);
        logoutItem.setBorder(new EmptyBorder(9, 12, 9, 36));
        logoutItem.addActionListener(e -> logout());
        popup.add(logoutItem);

        return popup;
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
            accessTimer.stop();
            authService.logout();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    // Signs the user out when a manager has deactivated or removed their access.
    private void checkAccess() {
        if (new UserDAO().isActive(authService.getCurrentUser().getId())) {
            return;
        }
        accessTimer.stop();
        JOptionPane.showMessageDialog(this, "Your access has been removed. Please contact the manager.",
                "Access removed", JOptionPane.WARNING_MESSAGE);
        authService.logout();
        dispose();
        new LoginFrame().setVisible(true);
    }
}
