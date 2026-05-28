package org.yourcompany.yourproject.gui.tabs;

import org.yourcompany.yourproject.ManagerService;
import org.yourcompany.yourproject.ManagerService.ManufacturerOrderLine;
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
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ManagerTab extends JPanel {
    private final ManagerService managerService = new ManagerService();

    public ManagerTab() {
        setLayout(new BorderLayout(8, 8));

        JTextArea outputArea = new JTextArea(20, 80);
        outputArea.setEditable(false);

        JPanel controls = new JPanel(new GridLayout(0, 1, 4, 4));

        JPanel reportRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        LocalDate today = LocalDate.now();
        JTextField yearField = new JTextField(String.valueOf(today.getYear()), 6);
        JTextField monthField = new JTextField(String.valueOf(today.getMonthValue()), 4);
        JButton reportButton = new JButton("Monthly Report");
        reportRow.add(new JLabel("Year"));
        reportRow.add(yearField);
        reportRow.add(new JLabel("Month"));
        reportRow.add(monthField);
        reportRow.add(reportButton);
        controls.add(reportRow);

        JPanel priceRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField stockField = new JTextField(10);
        JTextField priceField = new JTextField(8);
        JButton priceButton = new JButton("Change Price");
        priceRow.add(new JLabel("Stock #"));
        priceRow.add(stockField);
        priceRow.add(new JLabel("Price"));
        priceRow.add(priceField);
        priceRow.add(priceButton);
        controls.add(priceRow);

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField customerField = new JTextField("Lkim", 8);
        JTextField statusField = new JTextField("GOLD", 8);
        JButton statusButton = new JButton("Set Status");
        statusRow.add(new JLabel("Customer"));
        statusRow.add(customerField);
        statusRow.add(new JLabel("Status"));
        statusRow.add(statusField);
        statusRow.add(statusButton);
        controls.add(statusRow);

        JPanel maintenanceRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton deleteOldButton = new JButton("Delete Old Sales");
        maintenanceRow.add(new JLabel("Maintenance:"));
        maintenanceRow.add(deleteOldButton);
        controls.add(maintenanceRow);

        JPanel orderRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField mfrField = new JTextField(8);
        JTextField orderStockField = new JTextField(8);
        JTextField orderQtyField = new JTextField(4);
        JButton addLineButton = new JButton("Add Line");
        JButton sendOrderButton = new JButton("Send Order");
        JButton clearLinesButton = new JButton("Clear Lines");
        JLabel pendingLabel = new JLabel("Pending: 0 line(s)");
        orderRow.add(new JLabel("Mfr"));
        orderRow.add(mfrField);
        orderRow.add(new JLabel("Stock #"));
        orderRow.add(orderStockField);
        orderRow.add(new JLabel("Qty"));
        orderRow.add(orderQtyField);
        orderRow.add(addLineButton);
        orderRow.add(sendOrderButton);
        orderRow.add(clearLinesButton);
        orderRow.add(pendingLabel);
        controls.add(orderRow);

        add(controls, BorderLayout.NORTH);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        List<ManufacturerOrderLine> pendingLines = new ArrayList<>();
        final String[] pendingMfr = { null };

        reportButton.addActionListener(e -> GuiTaskRunner.run(
            "Monthly Report",
            outputArea,
            () -> ConsoleCapture.capture(() -> managerService.printMonthlySalesReport(
                Integer.parseInt(yearField.getText().trim()),
                Integer.parseInt(monthField.getText().trim())
            )),
            reportButton
        ));

        priceButton.addActionListener(e -> GuiTaskRunner.run(
            "Change Price",
            outputArea,
            () -> ConsoleCapture.capture(() -> managerService.changePrice(
                stockField.getText().trim(),
                Double.parseDouble(priceField.getText().trim())
            )),
            priceButton
        ));

        statusButton.addActionListener(e -> GuiTaskRunner.run(
            "Update Customer Status",
            outputArea,
            () -> ConsoleCapture.capture(() -> managerService.updateCustomerStatusManually(
                customerField.getText().trim(),
                statusField.getText().trim()
            )),
            statusButton
        ));

        deleteOldButton.addActionListener(e -> GuiTaskRunner.run(
            "Delete Old Sales",
            outputArea,
            () -> ConsoleCapture.capture(managerService::deleteOldSalesTransactions),
            deleteOldButton
        ));

        addLineButton.addActionListener(e -> GuiTaskRunner.run(
            "Add Order Line",
            outputArea,
            () -> ConsoleCapture.capture(() -> {
                String mfr = mfrField.getText().trim();
                String sn = orderStockField.getText().trim();
                String qtyStr = orderQtyField.getText().trim();
                if (mfr.isEmpty() || sn.isEmpty() || qtyStr.isEmpty()) {
                    System.out.println("Mfr, Stock #, and Qty are all required.");
                    return;
                }
                int qty;
                try {
                    qty = Integer.parseInt(qtyStr);
                } catch (NumberFormatException nfe) {
                    System.out.println("Qty must be an integer.");
                    return;
                }
                if (qty <= 0) {
                    System.out.println("Qty must be > 0.");
                    return;
                }
                if (pendingMfr[0] == null) {
                    pendingMfr[0] = mfr;
                } else if (!pendingMfr[0].equalsIgnoreCase(mfr)) {
                    System.out.println("All lines must be for the same manufacturer (current: " +
                                       pendingMfr[0] + "). Use Clear Lines to start a new order.");
                    return;
                }
                pendingLines.add(new ManufacturerOrderLine(sn, qty));
                pendingLabel.setText("Pending: " + pendingLines.size() + " line(s) for " + pendingMfr[0]);
                System.out.println("Added line: " + sn + " x " + qty);
                orderStockField.setText("");
                orderQtyField.setText("");
            }),
            addLineButton
        ));

        clearLinesButton.addActionListener(e -> GuiTaskRunner.run(
            "Clear Order Lines",
            outputArea,
            () -> ConsoleCapture.capture(() -> {
                pendingLines.clear();
                pendingMfr[0] = null;
                pendingLabel.setText("Pending: 0 line(s)");
                System.out.println("Cleared pending lines.");
            }),
            clearLinesButton
        ));

        sendOrderButton.addActionListener(e -> GuiTaskRunner.run(
            "Send Manufacturer Order",
            outputArea,
            () -> ConsoleCapture.capture(() -> {
                if (pendingLines.isEmpty()) {
                    System.out.println("No pending lines. Use Add Line first.");
                    return;
                }
                try {
                    managerService.sendManufacturerOrder(pendingMfr[0], new ArrayList<>(pendingLines));
                    pendingLines.clear();
                    pendingMfr[0] = null;
                    pendingLabel.setText("Pending: 0 line(s)");
                } catch (Exception ex) {
                    System.out.println("Failed: " + ex.getMessage());
                }
            }),
            sendOrderButton
        ));
    }
}
