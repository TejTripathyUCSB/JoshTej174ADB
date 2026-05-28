package org.yourcompany.yourproject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CartService {

    public void addItem(String customerId, String stockNumber, int quantity) {
        String sql = """
            MERGE INTO emart_cart_items c
            USING (SELECT ? AS customer_id, ? AS stock_number, ? AS quantity FROM dual) input
            ON (c.customer_id = input.customer_id AND c.stock_number = input.stock_number)
            WHEN MATCHED THEN
                UPDATE SET c.quantity = c.quantity + input.quantity
            WHEN NOT MATCHED THEN
                INSERT (customer_id, stock_number, quantity)
                VALUES (input.customer_id, input.stock_number, input.quantity)
        """;

        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, customerId);
            stmt.setString(2, stockNumber);
            stmt.setInt(3, quantity);
            stmt.executeUpdate();

            System.out.println("Added item to cart.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeItem(String customerId, String stockNumber) {
        String sql = """
            DELETE FROM emart_cart_items
            WHERE customer_id = ?
              AND stock_number = ?
        """;

        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, customerId);
            stmt.setString(2, stockNumber);

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                System.out.println("Removed item from cart.");
            } else {
                System.out.println("Item was not in cart.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void displayCart(String customerId) {

        String sql = """
            SELECT c.stock_number, i.manufacturer_name, i.model_number,
                   c.quantity, p.price, c.quantity * p.price AS line_total
            FROM emart_cart_items c
            JOIN emart_products p ON c.stock_number = p.stock_number
            JOIN item i           ON c.stock_number = i.stock_number
            WHERE c.customer_id = ?
            ORDER BY c.stock_number
        """;

        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, customerId);

            try (ResultSet rs = stmt.executeQuery()) {
                boolean found = false;
                double total = 0;

                System.out.println("Cart for customer " + customerId + ":");

                while (rs.next()) {
                    found = true;
                    double lineTotal = rs.getDouble("line_total");
                    total += lineTotal;

                    System.out.println(
                        rs.getString("stock_number") + " | " +
                        rs.getString("manufacturer_name") + " " +
                        rs.getString("model_number") + " | qty=" +
                        rs.getInt("quantity") + " | price=$" +
                        rs.getDouble("price") + " | line=$" +
                        lineTotal
                    );
                }

                if (!found) {
                    System.out.println("Cart is empty.");
                } else {
                    System.out.println("Subtotal: $" + total);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearCart(String customerId) {
        String sql = "DELETE FROM emart_cart_items WHERE customer_id = ?";

        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, customerId);
            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
