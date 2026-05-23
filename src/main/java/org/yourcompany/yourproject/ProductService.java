package org.yourcompany.yourproject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ProductService {

    public void listAllProducts() {
        String sql = """
            SELECT stock_number, category, manufacturer, model_number, price
            FROM emart_products
            ORDER BY stock_number
        """;

        runProductQuery(sql);
    }

    public void searchByCategory(String category) {
        String sql = """
            SELECT stock_number, category, manufacturer, model_number, price
            FROM emart_products
            WHERE LOWER(category) = LOWER(?)
        """;

        runProductQueryWithOneParam(sql, category);
    }

    public void searchByManufacturer(String manufacturer) {
        String sql = """
            SELECT stock_number, category, manufacturer, model_number, price
            FROM emart_products
            WHERE LOWER(manufacturer) = LOWER(?)
        """;

        runProductQueryWithOneParam(sql, manufacturer);
    }

    public void searchByStockNumber(String stockNumber) {
        String sql = """
            SELECT stock_number, category, manufacturer, model_number, price
            FROM emart_products
            WHERE stock_number = ?
        """;

        runProductQueryWithOneParam(sql, stockNumber);
    }

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

    public void searchByAttribute(String attributeName, String attributeValue) {
    String sql = """
        SELECT p.stock_number, p.category, p.manufacturer, p.model_number, p.price
        FROM emart_products p
        JOIN emart_product_attributes a
          ON p.stock_number = a.stock_number
        WHERE LOWER(a.attribute_name) = LOWER(?)
          AND LOWER(a.attribute_value) = LOWER(?)
    """;

    runProductQueryWithTwoParams(sql, attributeName, attributeValue);
}

public void searchCompatibleItems(String manufacturer, String modelNumber) {
    String sql = """
        SELECT p.stock_number, p.category, p.manufacturer, p.model_number, p.price
        FROM emart_products p
        JOIN emart_compatibility c
          ON p.stock_number = c.stock_number
        WHERE LOWER(c.compatible_manufacturer) = LOWER(?)
          AND LOWER(c.compatible_model_number) = LOWER(?)
    """;

    runProductQueryWithTwoParams(sql, manufacturer, modelNumber);
}

private void runProductQueryWithTwoParams(String sql, String value1, String value2) {
    try (Connection conn = DB.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, value1);
        stmt.setString(2, value2);

        try (ResultSet rs = stmt.executeQuery()) {
            printProducts(rs);
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}

}