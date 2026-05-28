package org.yourcompany.yourproject.gui;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import org.yourcompany.yourproject.gui.tabs.CartCheckoutTab;
import org.yourcompany.yourproject.gui.tabs.ManagerTab;
import org.yourcompany.yourproject.gui.tabs.OrderDepotTab;
import org.yourcompany.yourproject.gui.tabs.ProductTab;
import org.yourcompany.yourproject.gui.tabs.ShippingTab;

public class AppFrame extends JFrame {

    public AppFrame() {
        super("174A PROJECT: GUI Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Products", new ProductTab());
        tabs.addTab("Cart/Checkout", new CartCheckoutTab());
        tabs.addTab("Orders/eDEPOT", new OrderDepotTab());
        tabs.addTab("Shipping", new ShippingTab());
        tabs.addTab("Manager", new ManagerTab());

        add(tabs, BorderLayout.CENTER);
    }
}
