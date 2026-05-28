package org.yourcompany.yourproject.gui;

import javax.swing.SwingUtilities;

public class GuiMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GuiMain::showLogin);
    }

    private static void showLogin() {
        LoginPage login = new LoginPage(session -> {
            AppFrame frame = new AppFrame(session, GuiMain::showLogin);
            frame.setVisible(true);
        });
        login.setVisible(true);
    }
}
