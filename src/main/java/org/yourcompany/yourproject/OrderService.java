package org.yourcompany.yourproject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OrderService {

    public void checkout(String customerId) {
        try (Connection conn = DB.getConnection()) {

            double subtotal = calculateSubtotal(conn, customerId);

            if (subtotal <= 0) {
                System.out.println("Cart is empty. Cannot checkout.");
                return;
            }

            String status = getCustomerStatus(conn, customerId);

            double discountPercent = getDiscountPercent(conn, status);
            double discount = subtotal * (discountPercent / 100.0);

            double freeShipMin = getRule(conn, "FREE_SHIPPING_THRESHOLD");
            double shipPercent = getRule(conn, "SHIPPING_PERCENT");

            double shipping = 0;
            if (!(subtotal > freeShipMin || status.equals("NEW"))) {
                shipping = subtotal * (shipPercent / 100.0);
            }

            double total = subtotal - discount + shipping;

            int orderId = createOrder(conn, customerId, subtotal, discount, shipping, total);
            copyCartToOrderItems(conn, customerId, orderId);
            clearCart(conn, customerId);

            System.out.println("Checkout complete.");
            System.out.println("Order ID: " + orderId);
            System.out.println("Subtotal: $" + subtotal);
            System.out.println("Discount: $" + discount);
            System.out.println("Shipping: $" + shipping);
            System.out.println("Total: $" + total);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void displayOrder(int orderId) {
        String sql = """
            SELECT o.order_id, o.customer_id, o.order_date,
                   o.subtotal, o.discount, o.shipping_fee, o.total,
                   i.stock_number, p.manufacturer, p.model_number,
                   i.quantity, i.price_each
            FROM emart_orders o
            JOIN emart_order_items i ON o.order_id = i.order_id
            JOIN emart_products p ON i.stock_number = p.stock_number
            WHERE o.order_id = ?
            ORDER BY i.stock_number
        """;

        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orderId);

            try (ResultSet rs = stmt.executeQuery()) {
                boolean found = false;

                while (rs.next()) {
                    if (!found) {
                        found = true;
                        System.out.println("Order #" + rs.getInt("order_id"));
                        System.out.println("Customer: " + rs.getString("customer_id"));
                        System.out.println("Subtotal: $" + rs.getDouble("subtotal"));
                        System.out.println("Discount: $" + rs.getDouble("discount"));
                        System.out.println("Shipping: $" + rs.getDouble("shipping_fee"));
                        System.out.println("Total: $" + rs.getDouble("total"));
                        System.out.println("Items:");
                    }

                    System.out.println(
                        rs.getString("stock_number") + " | " +
                        rs.getString("manufacturer") + " " +
                        rs.getString("model_number") + " | qty=" +
                        rs.getInt("quantity") + " | price=$" +
                        rs.getDouble("price_each")
                    );
                }

                if (!found) {
                    System.out.println("Order not found.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double calculateSubtotal(Connection conn, String customerId) throws SQLException {
        String sql = """
            SELECT NVL(SUM(c.quantity * p.price), 0) AS subtotal
            FROM emart_cart_items c
            JOIN emart_products p ON c.stock_number = p.stock_number
            WHERE c.customer_id = ?
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getDouble("subtotal");
            }
        }
    }

    private String getCustomerStatus(Connection conn, String customerId) throws SQLException {
        String sql = "SELECT status FROM emart_customers WHERE customer_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("status");
                throw new SQLException("Customer not found: " + customerId);
            }
        }
    }

   private double getDiscountPercent(Connection conn, String status) throws SQLException {
    return switch (status) {
        case "NEW" -> getRule(conn, "NEW_CUSTOMER_DISCOUNT_PERCENT");
        case "GOLD" -> getRule(conn, "GOLD_DISCOUNT_PERCENT");
        case "SILVER" -> getRule(conn, "SILVER_DISCOUNT_PERCENT");
        case "GREEN" -> getRule(conn, "GREEN_DISCOUNT_PERCENT");
        default -> 0;
    };
}

    private double getRule(Connection conn, String ruleName) throws SQLException {
        String sql = "SELECT rule_value FROM emart_rules WHERE rule_name = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ruleName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getDouble("rule_value");
                throw new SQLException("Rule not found: " + ruleName);
            }
        }
    }

    private int createOrder(Connection conn, String customerId, double subtotal,
                            double discount, double shipping, double total) throws SQLException {
        String sql = """
            INSERT INTO emart_orders
            (customer_id, subtotal, discount, shipping_fee, total)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, new String[]{"ORDER_ID"})) {
            stmt.setString(1, customerId);
            stmt.setDouble(2, subtotal);
            stmt.setDouble(3, discount);
            stmt.setDouble(4, shipping);
            stmt.setDouble(5, total);

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("Could not retrieve generated order ID.");
            }
        }
    }

    private void copyCartToOrderItems(Connection conn, String customerId, int orderId) throws SQLException {
        String sql = """
            INSERT INTO emart_order_items (order_id, stock_number, quantity, price_each)
            SELECT ?, c.stock_number, c.quantity, p.price
            FROM emart_cart_items c
            JOIN emart_products p ON c.stock_number = p.stock_number
            WHERE c.customer_id = ?
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            stmt.setString(2, customerId);
            stmt.executeUpdate();
        }
    }

    private void clearCart(Connection conn, String customerId) throws SQLException {
        String sql = "DELETE FROM emart_cart_items WHERE customer_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customerId);
            stmt.executeUpdate();
        }
    }
}