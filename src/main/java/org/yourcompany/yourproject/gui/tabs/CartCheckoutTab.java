package org.yourcompany.yourproject.gui.tabs;

import org.yourcompany.yourproject.CartService;
import org.yourcompany.yourproject.OrderService;
import org.yourcompany.yourproject.gui.util.ConsoleCapture;
import org.yourcompany.yourproject.gui.util.GuiTaskRunner;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class CartCheckoutTab extends JPanel {
    private final CartService cartService = new CartService();
    private final OrderService orderService = new OrderService();

    public CartCheckoutTab() {
        this("");
    }

    public CartCheckoutTab(String customerId) {
        setLayout(new BorderLayout(8, 8));

        JTextArea outputArea = new JTextArea(20, 80);
        outputArea.setEditable(false);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JTextField customerField = new JTextField(customerId == null ? "" : customerId, 8);
        customerField.setEditable(false);
        customerField.setToolTipText("Logged-in customer (locked to current session)");
        JTextField stockField = new JTextField(10);
        JTextField quantityField = new JTextField("1", 4);

        JButton addButton = new JButton("Add");
        JButton removeButton = new JButton("Remove");
        JButton displayButton = new JButton("Display Cart");
        JButton checkoutButton = new JButton("Checkout");

        controls.add(new JLabel("Customer"));
        controls.add(customerField);
        controls.add(new JLabel("Stock #"));
        controls.add(stockField);
        controls.add(new JLabel("Qty"));
        controls.add(quantityField);
        controls.add(addButton);
        controls.add(removeButton);
        controls.add(displayButton);
        controls.add(checkoutButton);

        add(controls, BorderLayout.NORTH);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        addButton.addActionListener(e -> GuiTaskRunner.run(
            "Add To Cart",
            outputArea,
            () -> {
                int qty = Integer.parseInt(quantityField.getText().trim());
                return ConsoleCapture.capture(() -> cartService.addItem(
                    customerField.getText().trim(),
                    stockField.getText().trim(),
                    qty
                ));
            },
            addButton
        ));

        removeButton.addActionListener(e -> GuiTaskRunner.run(
            "Remove From Cart",
            outputArea,
            () -> ConsoleCapture.capture(() -> cartService.removeItem(
                customerField.getText().trim(),
                stockField.getText().trim()
            )),
            removeButton
        ));

        displayButton.addActionListener(e -> GuiTaskRunner.run(
            "Display Cart",
            outputArea,
            () -> ConsoleCapture.capture(() -> cartService.displayCart(customerField.getText().trim())),
            displayButton
        ));

        checkoutButton.addActionListener(e -> GuiTaskRunner.run(
            "Checkout",
            outputArea,
            () -> ConsoleCapture.capture(() -> {
                Integer orderId = orderService.checkout(customerField.getText().trim());
                if (orderId != null) {
                    System.out.println("Created order_id=" + orderId);
                }
            }),
            checkoutButton
        ));
    }
}
