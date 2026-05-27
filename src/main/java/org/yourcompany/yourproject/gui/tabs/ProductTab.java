package org.yourcompany.yourproject.gui.tabs;

import org.yourcompany.yourproject.ProductService;
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

public class ProductTab extends JPanel {
    private final ProductService productService = new ProductService();

    public ProductTab() {
        setLayout(new BorderLayout(8, 8));

        JTextArea outputArea = new JTextArea(20, 80);
        outputArea.setEditable(false);

        JPanel controls = new JPanel(new GridLayout(0, 1, 6, 6));

        JButton listAllButton = new JButton("List All Products");
        controls.add(listAllButton);

        JPanel stockRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField stockField = new JTextField(12);
        JButton searchStockButton = new JButton("Search Stock #");
        stockRow.add(new JLabel("Stock #"));
        stockRow.add(stockField);
        stockRow.add(searchStockButton);
        controls.add(stockRow);

        JPanel manufacturerRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField manufacturerField = new JTextField(12);
        JButton searchManufacturerButton = new JButton("Search Manufacturer");
        manufacturerRow.add(new JLabel("Manufacturer"));
        manufacturerRow.add(manufacturerField);
        manufacturerRow.add(searchManufacturerButton);
        controls.add(manufacturerRow);

        JPanel categoryRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField categoryField = new JTextField(12);
        JButton searchCategoryButton = new JButton("Search Category");
        categoryRow.add(new JLabel("Category"));
        categoryRow.add(categoryField);
        categoryRow.add(searchCategoryButton);
        controls.add(categoryRow);

        JPanel modelRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField modelField = new JTextField(12);
        JButton searchModelButton = new JButton("Search Model");
        modelRow.add(new JLabel("Model"));
        modelRow.add(modelField);
        modelRow.add(searchModelButton);
        controls.add(modelRow);

        JPanel compatibleRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField compatMfrField = new JTextField(10);
        JTextField compatModelField = new JTextField(10);
        JButton searchCompatibleButton = new JButton("Search Compatible");
        compatibleRow.add(new JLabel("Compat. Mfr"));
        compatibleRow.add(compatMfrField);
        compatibleRow.add(new JLabel("Model"));
        compatibleRow.add(compatModelField);
        compatibleRow.add(searchCompatibleButton);
        controls.add(compatibleRow);

        JPanel attributeRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField attributeNameField = new JTextField(10);
        JTextField attributeValueField = new JTextField(10);
        JButton searchAttributeButton = new JButton("Search Attribute");
        attributeRow.add(new JLabel("Attr"));
        attributeRow.add(attributeNameField);
        attributeRow.add(new JLabel("Value"));
        attributeRow.add(attributeValueField);
        attributeRow.add(searchAttributeButton);
        controls.add(attributeRow);

        JPanel combinedRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField cStockField = new JTextField(7);
        JTextField cMfrField = new JTextField(7);
        JTextField cModelField = new JTextField(7);
        JTextField cCategoryField = new JTextField(7);
        JTextField cAttrNameField = new JTextField(7);
        JTextField cAttrValueField = new JTextField(7);
        JButton searchCombinedButton = new JButton("Combined Search");
        combinedRow.add(new JLabel("Combo:"));
        combinedRow.add(new JLabel("Stock"));
        combinedRow.add(cStockField);
        combinedRow.add(new JLabel("Mfr"));
        combinedRow.add(cMfrField);
        combinedRow.add(new JLabel("Model"));
        combinedRow.add(cModelField);
        combinedRow.add(new JLabel("Cat"));
        combinedRow.add(cCategoryField);
        combinedRow.add(new JLabel("Attr"));
        combinedRow.add(cAttrNameField);
        combinedRow.add(new JLabel("Val"));
        combinedRow.add(cAttrValueField);
        combinedRow.add(searchCombinedButton);
        controls.add(combinedRow);

        add(controls, BorderLayout.NORTH);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        listAllButton.addActionListener(e -> GuiTaskRunner.run(
            "List All Products",
            outputArea,
            () -> ConsoleCapture.capture(productService::listAllProducts),
            listAllButton
        ));

        searchStockButton.addActionListener(e -> GuiTaskRunner.run(
            "Search By Stock",
            outputArea,
            () -> ConsoleCapture.capture(() -> productService.searchByStockNumber(stockField.getText().trim())),
            searchStockButton
        ));

        searchManufacturerButton.addActionListener(e -> GuiTaskRunner.run(
            "Search By Manufacturer",
            outputArea,
            () -> ConsoleCapture.capture(() -> productService.searchByManufacturer(manufacturerField.getText().trim())),
            searchManufacturerButton
        ));

        searchCategoryButton.addActionListener(e -> GuiTaskRunner.run(
            "Search By Category",
            outputArea,
            () -> ConsoleCapture.capture(() -> productService.searchByCategory(categoryField.getText().trim())),
            searchCategoryButton
        ));

        searchModelButton.addActionListener(e -> GuiTaskRunner.run(
            "Search By Model",
            outputArea,
            () -> ConsoleCapture.capture(() -> productService.searchByModelNumber(modelField.getText().trim())),
            searchModelButton
        ));

        searchCompatibleButton.addActionListener(e -> GuiTaskRunner.run(
            "Search Compatible Items",
            outputArea,
            () -> ConsoleCapture.capture(() -> productService.searchCompatibleItems(
                compatMfrField.getText().trim(),
                compatModelField.getText().trim()
            )),
            searchCompatibleButton
        ));

        searchAttributeButton.addActionListener(e -> GuiTaskRunner.run(
            "Search By Attribute",
            outputArea,
            () -> ConsoleCapture.capture(() -> productService.searchByAttribute(
                attributeNameField.getText().trim(),
                attributeValueField.getText().trim()
            )),
            searchAttributeButton
        ));

        searchCombinedButton.addActionListener(e -> GuiTaskRunner.run(
            "Combined Search",
            outputArea,
            () -> ConsoleCapture.capture(() -> productService.searchCombined(
                cStockField.getText().trim(),
                cMfrField.getText().trim(),
                cModelField.getText().trim(),
                cCategoryField.getText().trim(),
                cAttrNameField.getText().trim(),
                cAttrValueField.getText().trim()
            )),
            searchCombinedButton
        ));
    }
}
