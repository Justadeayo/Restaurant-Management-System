/*
 * Restaurant Management System
 */
package com.groupfour.RMS;

import com.formdev.flatlaf.FlatLightLaf;
import com.groupfour.RMS.ui.LoginFrame;
import com.groupfour.RMS.ui.theme.Theme;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        // FlatLaf must be installed before any Swing component is created.
        FlatLightLaf.setup();
        Theme.install();

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
