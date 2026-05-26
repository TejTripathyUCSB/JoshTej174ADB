package org.yourcompany.yourproject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ProductService {

    // Standard SELECT used by all product-listing queries.
    // JOINs item because manufacturer + model live there now.
    private static final String PRODUCT_SELECT = """
        SELECT p.stock_number, p.category, i.manufacturer_name AS manufacturer,
               i.model_number, p.price
        FROM emart_products p
        JOIN item i ON p.stock_number = i.stock_number
    """;

    public void listAllProducts() {
        runProductQuery(PRODUCT_SELECT + " ORDER BY p.stock_number");
    }

    public void searchByCategory(String category) {
        runProductQueryWithOneParam(
            PRODUCT_SELECT + " WHERE LOWER(p.category) = LOWER(?)",
            category
        );
    }

    public void searchByManufacturer(String manufacturer) {
        runProductQueryWithOneParam(
            PRODUCT_SELECT + " WHERE LOWER(i.manufacturer_name) = LOWER(?)",
            manufacturer
        );
    }

    public void searchByStockNumber(String stockNumber) {
        runProductQueryWithOneParam(
            PRODUCT_SELECT + " WHERE p.stock_number = ?",
            stockNumber
        );
    }

    public void searchByModelNumber(String modelNumber) {
        runProductQueryWithOneParam(
            PRODUCT_SELECT + " WHERE LOWER(i.model_number) = LOWER(?)",
            modelNumber
        );
    }

    public void searchByAttribute(String attributeName, String attributeValue) {
        String sql = PRODUCT_SELECT + """
            JOIN emart_product_attributes a ON p.stock_number = a.stock_number
            WHERE LOWER(a.attribute_name)  = LOWER(?)
              AND LOWER(a.attribute_value) = LOWER(?)
        """;
        runProductQueryWithTwoParams(sql, attributeName, attributeValue);
    }

    /**
     * Find products compatible with the item identified by (manufacturer, model).
     * Per spec: "this product can replace any product in the list".
     * So we want products p where p.stock_number can replace the target item.
     * I.e., find rows in emart_compatibility where compatible_stock_number is
     * the target item's stock number.
     */
    public void searchCompatibleItems(String manufacturer, String modelNumber) {
        String sql = PRODUCT_SELECT + """
            JOIN emart_compatibility c ON p.stock_number = c.stock_number
            JOIN item target           ON c.compatible_stock_number = target.stock_number
            WHERE LOWER(target.manufacturer_name) = LOWER(?)
              AND LOWER(target.model_number)     = LOWER(?)
        """;
        runProductQueryWithTwoParams(sql, manufacturer, modelNumber);
    }

    // ---------- shared helpers ----------

    private void runProductQuery(String sql) {
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            printProducts(rs);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runProductQueryWithOneParam(String sql, String value) {
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, value);
            try (ResultSet rs = stmt.executeQuery()) {
                printProducts(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runProductQueryWithTwoParams(String sql, String v1, String v2) {
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, v1);
            stmt.setString(2, v2);
            try (ResultSet rs = stmt.executeQuery()) {
                printProducts(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printProducts(ResultSet rs) throws Exception {
        boolean found = false;
        while (rs.next()) {
            found = true;
            System.out.println(
                rs.getString("stock_number") + " | " +
                rs.getString("category") + " | " +
                rs.getString("manufacturer") + " | " +
                rs.getString("model_number") + " | $" +
                rs.getDouble("price")
            );
        }
        if (!found) {
            System.out.println("No products found.");
        }
    }
}
