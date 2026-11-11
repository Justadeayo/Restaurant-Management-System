package com.groupfour.RMS.ui.panels;

import com.groupfour.RMS.model.Order;
import com.groupfour.RMS.model.OrderItem;
import com.groupfour.RMS.model.OrderStep;
import com.groupfour.RMS.model.User;
import com.groupfour.RMS.service.AuthService;
import com.groupfour.RMS.service.OrderService;
import com.groupfour.RMS.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

// A customer's own orders. Each one shows as pending until it is completed.
public class MyOrdersPanel extends JPanel {
    private final AuthService authService;
    private final OrderService orderService = new OrderService();
    private final DefaultTableModel model;
    private final JTable table;
    private List<Order> orders;

    public MyOrdersPanel(AuthService authService) {
        this.authService = authService;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        JButton refresh = Theme.secondaryButton("Refresh");
        refresh.addActionListener(e -> load());
        JButton track = Theme.primaryButton("Track order");
        track.addActionListener(e -> trackSelected());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.add(refresh);
        toolbar.add(track);
        add(Theme.headerWithToolbar("My Orders", "Double-click an order to see its progress.", toolbar), BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"Order", "Placed", "Status", "Total"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        Theme.styleTable(table);
        table.getColumnModel().getColumn(2).setCellRenderer(new StatusRenderer());
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    trackSelected();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        Theme.styleScrollPane(scroll);
        add(scroll, BorderLayout.CENTER);

        load();
    }

    private void load() {
        model.setRowCount(0);
        orders = orderService.getCustomerOrders(authService.getCurrentUser());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
        for (Order order : orders) {
            String placed = order.getCreatedAt() == null ? "" : order.getCreatedAt().format(fmt);
            String status = order.getStatus() == Order.OrderStatus.COMPLETED ? "Completed" : "• Order pending";
            model.addRow(new Object[]{"#" + order.getId(), placed, status, String.format("%.2f", order.getTotalAmount())});
        }
    }

    private void trackSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an order first.", "My Orders", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        openTracker(orders.get(row).getId());
    }

    private void openTracker(int orderId) {
        User customer = authService.getCurrentUser();
        Order order = orderService.getCustomerOrder(orderId, customer);
        if (order == null) {
            JOptionPane.showMessageDialog(this, "This order could not be found.", "My Orders", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Order #" + order.getId(), Dialog.ModalityType.APPLICATION_MODAL);
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(new EmptyBorder(16, 16, 16, 16));
        content.setBackground(Theme.SURFACE);

        JLabel badge = new JLabel(order.getStatus() == Order.OrderStatus.COMPLETED ? "✔  Completed" : "•  Order pending");
        badge.setFont(Theme.BODY_BOLD);
        badge.setForeground(order.getStatus() == Order.OrderStatus.COMPLETED ? Theme.SUCCESS : Theme.WARNING);
        content.add(badge, BorderLayout.NORTH);

        JPanel steps = new JPanel();
        steps.setLayout(new BoxLayout(steps, BoxLayout.Y_AXIS));
        steps.setOpaque(false);
        for (OrderStep step : order.progressSteps()) {
            String mark = switch (step.getState()) {
                case "done" -> "✔";
                case "current" -> "●";
                default -> "○";
            };
            JLabel line = new JLabel(mark + "   " + step.getLabel());
            line.setFont("todo".equals(step.getState()) ? Theme.BODY : Theme.BODY_BOLD);
            line.setForeground("done".equals(step.getState()) ? Theme.SUCCESS
                    : "current".equals(step.getState()) ? Theme.WARNING : Theme.TEXT_MUTED);
            line.setBorder(new EmptyBorder(4, 0, 4, 0));
            steps.add(line);
        }
        content.add(steps, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        StringBuilder items = new StringBuilder("<html>");
        for (OrderItem item : order.getOrderItems()) {
            items.append(item.getQuantity()).append(" x ").append(item.getItemName()).append("<br>");
        }
        items.append("<b>Total: ").append(String.format("%.2f", order.getTotalAmount())).append("</b></html>");
        JLabel summary = new JLabel(items.toString());
        summary.setFont(Theme.BODY);
        bottom.add(summary, BorderLayout.CENTER);

        if (order.canBePaid()) {
            JLabel arrived = new JLabel("Your rider has arrived. Pay now to complete your order.");
            arrived.setFont(Theme.BODY);
            JButton pay = Theme.successButton("Pay now");
            pay.addActionListener(e -> {
                try {
                    orderService.customerPay(orderId, customer);
                    dialog.dispose();
                    load();
                    JOptionPane.showMessageDialog(this, "Payment received. Thank you!", "Paid", JOptionPane.INFORMATION_MESSAGE);
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Payment", JOptionPane.WARNING_MESSAGE);
                }
            });
            JPanel payRow = new JPanel(new BorderLayout(8, 0));
            payRow.setOpaque(false);
            payRow.add(arrived, BorderLayout.CENTER);
            payRow.add(pay, BorderLayout.EAST);
            bottom.add(payRow, BorderLayout.SOUTH);
        }
        content.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(420, 380));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        load();
    }

    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            if (!selected) {
                boolean completed = "Completed".equals(value);
                c.setForeground(completed ? Theme.SUCCESS : Theme.WARNING);
            }
            return c;
        }
    }
}
