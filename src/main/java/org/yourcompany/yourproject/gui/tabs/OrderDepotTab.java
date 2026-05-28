package org.yourcompany.yourproject.gui.tabs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.yourcompany.yourproject.InventoryService;
import org.yourcompany.yourproject.gui.util.ConsoleCapture;
import org.yourcompany.yourproject.gui.util.GuiTaskRunner;

public class OrderDepotTab extends JPanel {
    private final InventoryService inventoryService = new InventoryService();

    public OrderDepotTab() {
        setLayout(new BorderLayout(8, 8));

        JTextArea outputArea = new JTextArea(20, 80);
        outputArea.setEditable(false);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField orderIdField = new JTextField(8);
        JTextField stockField = new JTextField(10);

        JButton fillOrderButton = new JButton("Fill Order");
        JButton inventoryButton = new JButton("Check Inventory");

        controls.add(new JLabel("Order ID"));
        controls.add(orderIdField);
        controls.add(fillOrderButton);
        controls.add(new JLabel("Stock #"));
        controls.add(stockField);
        controls.add(inventoryButton);

        add(controls, BorderLayout.NORTH);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);


        fillOrderButton.addActionListener(e -> GuiTaskRunner.run(
            "Fill Order",
            outputArea,
            () -> ConsoleCapture.capture(() -> inventoryService.fillOrder(parseOrderId(orderIdField))),
            fillOrderButton
        ));

        inventoryButton.addActionListener(e -> GuiTaskRunner.run(
            "Inventory Check",
            outputArea,
            () -> {
                String sn = stockField.getText().trim();
                InventoryService.InventoryDetails d = inventoryService.getInventoryDetails(sn);
                if (d == null) {
                    return "Inventory for " + sn + ": not found.\n";
                }
                return "Inventory for " + sn + ":\n" +
                       "  quantity      = " + d.quantity() + "\n" +
                       "  location      = " + d.location() + "\n" +
                       "  min/max stock = " + d.minStock() + " / " + d.maxStock() + "\n" +
                       "  replenishment = " + d.replenishment() + "\n";
            },
            inventoryButton
        ));
    }

    private int parseOrderId(JTextField orderIdField) {
        return Integer.parseInt(orderIdField.getText().trim());
    }
}
