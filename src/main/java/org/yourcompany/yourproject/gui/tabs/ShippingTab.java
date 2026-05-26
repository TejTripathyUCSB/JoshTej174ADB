package org.yourcompany.yourproject.gui.tabs;

import org.yourcompany.yourproject.NoticeItem;
import org.yourcompany.yourproject.ShippingNoticeService;
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
import java.util.List;

public class ShippingTab extends JPanel {
    private final ShippingNoticeService shippingNoticeService = new ShippingNoticeService();

    public ShippingTab() {
        setLayout(new BorderLayout(8, 8));

        JTextArea outputArea = new JTextArea(20, 80);
        outputArea.setEditable(false);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JTextField noticeIdField = new JTextField("N001", 10);
        JTextField manufacturerField = new JTextField("HP", 10);
        JTextField companyField = new JTextField("UPS", 8);
        JTextField modelField = new JTextField("K435", 10);
        JTextField qtyField = new JTextField("2", 4);

        JButton createNoticeButton = new JButton("Create Notice");
        JButton receiveShipmentButton = new JButton("Receive Shipment");

        controls.add(new JLabel("Notice ID"));
        controls.add(noticeIdField);
        controls.add(new JLabel("Manufacturer"));
        controls.add(manufacturerField);
        controls.add(new JLabel("Company"));
        controls.add(companyField);
        controls.add(new JLabel("Model"));
        controls.add(modelField);
        controls.add(new JLabel("Qty"));
        controls.add(qtyField);
        controls.add(createNoticeButton);
        controls.add(receiveShipmentButton);

        add(controls, BorderLayout.NORTH);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        createNoticeButton.addActionListener(e -> GuiTaskRunner.run(
            "Create Shipping Notice",
            outputArea,
            () -> ConsoleCapture.capture(() -> {
                NoticeItem item = new NoticeItem(
                    manufacturerField.getText().trim(),
                    modelField.getText().trim(),
                    Integer.parseInt(qtyField.getText().trim())
                );
                shippingNoticeService.receiveShippingNotice(
                    noticeIdField.getText().trim(),
                    manufacturerField.getText().trim(),
                    companyField.getText().trim(),
                    List.of(item)
                );
                System.out.println("Shipping notice inserted.");
            }),
            createNoticeButton
        ));

        receiveShipmentButton.addActionListener(e -> GuiTaskRunner.run(
            "Receive Shipment",
            outputArea,
            () -> ConsoleCapture.capture(() -> {
                shippingNoticeService.receiveShipment(noticeIdField.getText().trim());
                System.out.println("Shipment received.");
            }),
            receiveShipmentButton
        ));
    }
}
