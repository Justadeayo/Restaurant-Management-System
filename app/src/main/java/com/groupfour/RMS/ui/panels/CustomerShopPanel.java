package com.groupfour.RMS.ui.panels;

import com.groupfour.RMS.dao.MenuItemDAO;
import com.groupfour.RMS.model.MenuItem;
import com.groupfour.RMS.model.Order;
import com.groupfour.RMS.service.AuthService;
import com.groupfour.RMS.service.OrderService;
import com.groupfour.RMS.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Customers choose items and a delivery address, then place a remote order.
public class CustomerShopPanel extends JPanel {
    private final AuthService authService;
    private final OrderService orderService = new OrderService();
    private final MenuItemDAO menuItemDAO = new MenuItemDAO();

    private final List<MenuItem> items = new ArrayList<>();
    private final List<JSpinner> quantities = new ArrayList<>();
    private JTextField addressField;
    private JPanel itemList;

    public CustomerShopPanel(AuthService authService) {
        this.authService = authService;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        add(Theme.pageHeader("Order Online", "Choose what you want and where it should be delivered."), BorderLayout.NORTH);

        itemList = new JPanel(new GridBagLayout());
        itemList.setBackground(Theme.SURFACE);
        JScrollPane scroll = new JScrollPane(itemList);
        Theme.styleScrollPane(scroll);
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = Theme.card(new BorderLayout(10, 10));
        addressField = new JTextField();
        addressField.putClientProperty("JTextField.placeholderText", "Delivery address");
        JButton place = Theme.primaryButton("Place order");
        place.setPreferredSize(new Dimension(160, 38));
        place.addActionListener(e -> placeOrder());

        JPanel addressRow = new JPanel(new BorderLayout(8, 0));
        addressRow.setOpaque(false);
        JLabel addressLabel = new JLabel("Delivery address");
        addressLabel.setFont(Theme.BODY_BOLD);
        addressRow.add(addressLabel, BorderLayout.WEST);
        addressRow.add(addressField, BorderLayout.CENTER);

        bottom.add(addressRow, BorderLayout.CENTER);
        bottom.add(place, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        loadItems();
    }

    private void loadItems() {
        items.clear();
        quantities.clear();
        itemList.removeAll();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        for (MenuItem item : menuItemDAO.getAvailableMenuItems()) {
            items.add(item);

            gbc.gridy = row;
            gbc.gridx = 0;
            gbc.weightx = 1;
            JLabel name = new JLabel(item.getName() + "  (" + item.getCategory() + ")");
            name.setFont(Theme.BODY_BOLD);
            itemList.add(name, gbc);

            gbc.gridx = 1;
            gbc.weightx = 0;
            JLabel price = new JLabel(String.format("%.2f", item.getPrice()));
            price.setFont(Theme.BODY);
            itemList.add(price, gbc);

            gbc.gridx = 2;
            JSpinner quantity = new JSpinner(new SpinnerNumberModel(0, 0, 50, 1));
            quantity.setPreferredSize(new Dimension(70, 28));
            quantities.add(quantity);
            itemList.add(quantity, gbc);
            row++;
        }

        if (items.isEmpty()) {
            JLabel none = new JLabel("No dishes are available right now.");
            none.setFont(Theme.BODY);
            none.setForeground(Theme.TEXT_MUTED);
            gbc.gridx = 0; gbc.gridy = 0;
            itemList.add(none, gbc);
        }
        itemList.revalidate();
        itemList.repaint();
    }

    private void placeOrder() {
        Map<Integer, Integer> selected = new LinkedHashMap<>();
        for (int i = 0; i < items.size(); i++) {
            selected.put(items.get(i).getId(), (Integer) quantities.get(i).getValue());
        }
        try {
            int orderId = orderService.placeOrder(selected, null, addressField.getText().trim(),
                    Order.OrderType.REMOTE, authService.getCurrentUser());
            JOptionPane.showMessageDialog(this,
                    "Thank you! Your order #" + orderId + " has been placed.\nTrack it under My Orders.",
                    "Order placed", JOptionPane.INFORMATION_MESSAGE);
            addressField.setText("");
            loadItems();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Order", JOptionPane.WARNING_MESSAGE);
        }
    }
}
