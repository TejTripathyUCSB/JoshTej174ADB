package org.yourcompany.yourproject.gui;

import org.yourcompany.yourproject.AuthService;
import org.yourcompany.yourproject.gui.tabs.CartCheckoutTab;
import org.yourcompany.yourproject.gui.tabs.ManagerTab;
import org.yourcompany.yourproject.gui.tabs.OrderDepotTab;
import org.yourcompany.yourproject.gui.tabs.ProductTab;
import org.yourcompany.yourproject.gui.tabs.ShippingTab;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class AppFrame extends JFrame {

    public AppFrame(AuthService.Session session, Runnable onLogout) {
        super("174A PROJECT: GUI Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Products", new ProductTab());
        tabs.addTab("Cart/Checkout", new CartCheckoutTab(session.customerId));
        if (session.manager) {
            tabs.addTab("Orders/eDEPOT", new OrderDepotTab());
            tabs.addTab("Shipping", new ShippingTab());
            tabs.addTab("Manager", new ManagerTab());
        }

        add(buildHeader(session, onLogout), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildHeader(AuthService.Session session, Runnable onLogout) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        String role = session.manager ? "Manager" : "Customer";
        JLabel who = new JLabel("Logged in as: " + session.customerId + "  (" + role + ")");

        JButton logoutButton = new JButton("Log Out");
        logoutButton.addActionListener(e -> {
            dispose();
            if (onLogout != null) {
                onLogout.run();
            }
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.add(logoutButton);

        header.add(who, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }
}
