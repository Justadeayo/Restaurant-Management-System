package com.groupfour.RMS.ui.theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Central place for the RMS palette, typography and component styling.
 * Change a colour here once and every panel picks it up.
 */
public final class Theme {

    // ---- Palette ---------------------------------------------------------
    /** Deep restaurant green used for headers, primary buttons and selection. */
    public static final Color PRIMARY        = new Color(0x1E3A34);
    public static final Color PRIMARY_DARK   = new Color(0x142822);
    public static final Color PRIMARY_LIGHT  = new Color(0x2F5D52);

    /** Warm amber accent for highlights and primary actions. */
    public static final Color ACCENT         = new Color(0xE0A458);
    public static final Color ACCENT_HOVER   = new Color(0xCF9347);

    public static final Color BG             = new Color(0xF6F4EF);
    public static final Color SURFACE        = Color.WHITE;
    public static final Color BORDER         = new Color(0xDDD8CE);

    public static final Color TEXT           = new Color(0x24292B);
    public static final Color TEXT_MUTED     = new Color(0x6B7280);
    public static final Color TEXT_ON_DARK   = new Color(0xF7F5F0);

    public static final Color SUCCESS        = new Color(0x2E7D32);
    public static final Color DANGER         = new Color(0xC0392B);
    public static final Color WARNING        = new Color(0xED6C02);
    public static final Color ROW_ALT        = new Color(0xF3F1EC);

    // ---- Typography ------------------------------------------------------
    private static final String FONT_FAMILY = pickFont("Segoe UI", "Inter", "Roboto", "SansSerif");

    public static Font font(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    public static final Font TITLE    = font(Font.BOLD, 22);
    public static final Font SUBTITLE = font(Font.PLAIN, 13);
    public static final Font HEADING  = font(Font.BOLD, 15);
    public static final Font BODY     = font(Font.PLAIN, 13);
    public static final Font BODY_BOLD = font(Font.BOLD, 13);

    private Theme() {
    }

    private static String pickFont(String... candidates) {
        String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        for (String candidate : candidates) {
            for (String name : available) {
                if (name.equalsIgnoreCase(candidate)) {
                    return name;
                }
            }
        }
        return Font.SANS_SERIF;
    }

    // ---- Global defaults -------------------------------------------------
    /**
     * Applies the palette to the whole application through UIManager.
     * Call once after setting the FlatLaf look and feel.
     */
    public static void install() {
        UIManager.put("Panel.background", BG);
        UIManager.put("Viewport.background", SURFACE);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT);

        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.font", BODY);

        UIManager.put("Button.font", BODY_BOLD);
        UIManager.put("Button.arc", 10);
        UIManager.put("Button.focusWidth", 0);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.focusColor", ACCENT);
        UIManager.put("Component.accentColor", PRIMARY_LIGHT);

        UIManager.put("TextField.font", BODY);
        UIManager.put("PasswordField.font", BODY);
        UIManager.put("ComboBox.font", BODY);
        UIManager.put("CheckBox.font", BODY);

        UIManager.put("Table.font", BODY);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.rowHeight", 28);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("Table.selectionBackground", PRIMARY_LIGHT);
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("Table.alternateRowColor", ROW_ALT);

        UIManager.put("TableHeader.font", BODY_BOLD);
        UIManager.put("TableHeader.background", PRIMARY);
        UIManager.put("TableHeader.foreground", TEXT_ON_DARK);
        UIManager.put("TableHeader.separatorColor", PRIMARY_DARK);
        UIManager.put("TableHeader.bottomSeparatorColor", PRIMARY_DARK);

        UIManager.put("TabbedPane.font", BODY_BOLD);
        UIManager.put("TabbedPane.background", BG);
        UIManager.put("TabbedPane.foreground", TEXT);
        UIManager.put("TabbedPane.selectedBackground", SURFACE);
        UIManager.put("TabbedPane.selectedForeground", PRIMARY);
        UIManager.put("TabbedPane.underlineColor", ACCENT);
        UIManager.put("TabbedPane.tabHeight", 38);
        UIManager.put("TabbedPane.showTabSeparators", true);

        UIManager.put("MenuBar.background", PRIMARY);
        UIManager.put("MenuBar.foreground", TEXT_ON_DARK);
        UIManager.put("Menu.background", PRIMARY);
        UIManager.put("Menu.foreground", TEXT_ON_DARK);
        UIManager.put("Menu.selectionBackground", PRIMARY_LIGHT);
        UIManager.put("Menu.selectionForeground", Color.WHITE);
        UIManager.put("MenuItem.foreground", TEXT);

        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumbArc", 12);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));

        UIManager.put("TitledBorder.font", HEADING);
        UIManager.put("TitledBorder.titleColor", PRIMARY);
    }

    // ---- Reusable components --------------------------------------------

    /** Page header with a title and optional subtitle. */
    public static JPanel pageHeader(String title, String subtitle) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);
        header.setBorder(new EmptyBorder(12, 16, 6, 16));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(TITLE);
        titleLabel.setForeground(PRIMARY);
        header.add(titleLabel, BorderLayout.NORTH);

        if (subtitle != null && !subtitle.isBlank()) {
            JLabel subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setFont(SUBTITLE);
            subtitleLabel.setForeground(TEXT_MUTED);
            subtitleLabel.setBorder(new EmptyBorder(2, 0, 0, 0));
            header.add(subtitleLabel, BorderLayout.SOUTH);
        }
        return header;
    }

    /** Header + toolbar stacked together, for use as a panel's NORTH area. */
    public static JPanel headerWithToolbar(String title, String subtitle, JComponent toolbar) {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BG);
        top.add(pageHeader(title, subtitle), BorderLayout.NORTH);
        styleToolbar(toolbar);
        top.add(toolbar, BorderLayout.SOUTH);
        return top;
    }

    public static void styleToolbar(JComponent toolbar) {
        toolbar.setBackground(BG);
        toolbar.setBorder(new EmptyBorder(4, 12, 8, 12));
    }

    /** Solid primary (amber) action button. */
    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(ACCENT);
        button.setForeground(PRIMARY_DARK);
        button.setFont(BODY_BOLD);
        button.putClientProperty("JButton.buttonType", "roundRect");
        return button;
    }

    /** Neutral secondary button. */
    public static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(SURFACE);
        button.setForeground(TEXT);
        button.setFont(BODY);
        button.putClientProperty("JButton.buttonType", "roundRect");
        return button;
    }

    /** Solid green button (confirm/save). */
    public static JButton successButton(String text) {
        JButton button = primaryButton(text);
        button.setBackground(PRIMARY);
        button.setForeground(TEXT_ON_DARK);
        return button;
    }

    /** Destructive action button. */
    public static JButton dangerButton(String text) {
        JButton button = primaryButton(text);
        button.setBackground(DANGER);
        button.setForeground(Color.WHITE);
        return button;
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(28);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(BORDER);
        table.setSelectionBackground(PRIMARY_LIGHT);
        table.setSelectionForeground(Color.WHITE);
        table.setFont(BODY);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(PRIMARY);
        header.setForeground(TEXT_ON_DARK);
        header.setFont(BODY_BOLD);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 32));
        header.setReorderingAllowed(false);
    }

    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(SURFACE);
    }

    /** White card with a subtle border, useful for login/dialog content. */
    public static JPanel card(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(20, 24, 20, 24)));
        return panel;
    }
}
