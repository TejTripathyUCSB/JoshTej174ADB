package org.yourcompany.yourproject.gui.tabs;

import org.yourcompany.yourproject.ManagerService;
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
import java.time.LocalDate;

public class ManagerTab extends JPanel {
    private final ManagerService managerService = new ManagerService();

    public ManagerTab() {
        setLayout(new BorderLayout(8, 8));

        JTextArea outputArea = new JTextArea(20, 80);
        outputArea.setEditable(false);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));

        LocalDate today = LocalDate.now();
        JTextField yearField = new JTextField(String.valueOf(today.getYear()), 6);
        JTextField monthField = new JTextField(String.valueOf(today.getMonthValue()), 4);
        JButton reportButton = new JButton("Monthly Report");

        JTextField stockField = new JTextField(10);
        JTextField priceField = new JTextField(8);
        JButton priceButton = new JButton("Change Price");

        JTextField customerField = new JTextField("Lkim", 8);
        JTextField statusField = new JTextField("GOLD", 8);
        JButton statusButton = new JButton("Set Status");

        controls.add(new JLabel("Year"));
        controls.add(yearField);
        controls.add(new JLabel("Month"));
        controls.add(monthField);
        controls.add(reportButton);

        controls.add(new JLabel("Stock #"));
        controls.add(stockField);
        controls.add(new JLabel("Price"));
        controls.add(priceField);
        controls.add(priceButton);

        controls.add(new JLabel("Customer"));
        controls.add(customerField);
        controls.add(new JLabel("Status"));
        controls.add(statusField);
        controls.add(statusButton);

        add(controls, BorderLayout.NORTH);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

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
    }
}
