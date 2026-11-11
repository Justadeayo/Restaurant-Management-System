package com.groupfour.RMS.ui.theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

public final class PasswordToggle {
    private PasswordToggle() {
    }

    // Puts an eye button beside a password field. Clicking it shows or hides the password.
    public static JPanel wrap(JPasswordField field) {
        char hiddenChar = field.getEchoChar() == 0 ? '•' : field.getEchoChar();

        JButton eye = new JButton(new EyeIcon(false));
        eye.setToolTipText("Show password");
        eye.setFocusPainted(false);
        eye.setBorderPainted(false);
        eye.setContentAreaFilled(false);
        eye.setMargin(new Insets(0, 6, 0, 6));
        eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eye.addActionListener(e -> {
            boolean wasShowing = field.getEchoChar() == 0;
            field.setEchoChar(wasShowing ? hiddenChar : (char) 0);
            boolean showingNow = !wasShowing;
            eye.setIcon(new EyeIcon(showingNow));
            eye.setToolTipText(showingNow ? "Hide password" : "Show password");
        });

        JPanel panel = new JPanel(new BorderLayout(4, 0));
        panel.setOpaque(false);
        panel.add(field, BorderLayout.CENTER);
        panel.add(eye, BorderLayout.EAST);
        return panel;
    }

    // Eye outline with a pupil. When slashed, the password is visible and clicking hides it.
    private static final class EyeIcon implements Icon {
        private final boolean slashed;

        EyeIcon(boolean slashed) {
            this.slashed = slashed;
        }

        @Override
        public int getIconWidth() {
            return 20;
        }

        @Override
        public int getIconHeight() {
            return 20;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.TEXT_MUTED);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            double left = x + 2, right = x + 18, top = y + 5, bottom = y + 15, midY = y + 10;
            Path2D outline = new Path2D.Double();
            outline.moveTo(left, midY);
            outline.curveTo(left + 3, top, right - 3, top, right, midY);
            outline.curveTo(right - 3, bottom, left + 3, bottom, left, midY);
            outline.closePath();
            g2.draw(outline);
            g2.draw(new Ellipse2D.Double(x + 7.5, y + 7.5, 5, 5));

            if (slashed) {
                g2.draw(new Line2D.Double(x + 3, y + 17, x + 17, y + 3));
            }
            g2.dispose();
        }
    }
}
