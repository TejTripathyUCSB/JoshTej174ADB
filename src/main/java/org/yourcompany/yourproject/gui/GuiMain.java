package org.yourcompany.yourproject.gui;

import javax.swing.SwingUtilities;

public class GuiMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppFrame frame = new AppFrame();
            frame.setVisible(true);
        });
    }
}
