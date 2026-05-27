package org.yourcompany.yourproject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OrderService {

    public Integer checkout(String customerId) {
        Connection conn = null;
        try {
            conn = DB.getConnection();
            conn.setAutoCommit(false);

            double subtotal = calculateSubtotal(conn, customerId);
            if (subtotal <= 0) {
                System.out.println("Cart is empty. Cannot checkout.");
                conn.rollback();
                return null;
            }

            String status = getCustomerStatus(conn, customerId);
            double discountPercent = getDiscountPercent(conn, status);
            double discount = subtotal * (discountPercent / 100.0);

            double freeShipMin = getRule(conn, "FREE_SHIPPING_THRESHOLD");
            double shipPercent = getRule(conn, "SHIPPING_PERCENT");
            double shipping = (subtotal > freeShipMin || status.equals("NEW"))
                    ? 0
                    : subtotal * (shipPercent / 100.0);

            double total = subtotal - discount + shipping;

            int orderId = createOrder(conn, customerId, subtotal, discount, shipping, total);
            copyCartToOrderItems(conn, customerId, orderId);
            clearCart(conn, customerId);
            updateCustomerStatus(conn, customerId);

            conn.commit();

            System.out.println("Checkout complete.");
            System.out.println("Order ID: " + orderId);
            System.out.printf("Subtotal: $%.2f%n", subtotal);
            System.out.printf("Discount: $%.2f%n", discount);
            System.out.printf("Shipping: $%.2f%n", shipping);
            System.out.printf("Total:    $%.2f%n", total);
            return orderId;

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return null;
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    public void displayOrder(int orderId) {
        String sql = """
            SELECT o.order_id, o.customer_id, o.order_date,
                   o.subtotal, o.discount, o.shipping_fee, o.total,
                   o.fulfillment_status,
                   oi.stock_number, i.manufacturer_name, i.model_number,
                   oi.quantity, oi.price_each
            FROM emart_orders o
            JOIN emart_order_items oi ON o.order_id = oi.order_id
            JOIN item i ON oi.stock_number = i.stock_number
            WHERE o.order_id = ?
            ORDER BY oi.stock_number
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
                        System.out.println("Date: " + rs.getDate("order_date"));
                        System.out.println("Status: " + rs.getString("fulfillment_status"));
                        System.out.printf("Subtotal: $%.2f%n", rs.getDouble("subtotal"));
                        System.out.printf("Discount: $%.2f%n", rs.getDouble("discount"));
                        System.out.printf("Shipping: $%.2f%n", rs.getDouble("shipping_fee"));
                        System.out.printf("Total:    $%.2f%n", rs.getDouble("total"));
                        System.out.println("Items:");
                    }

                    System.out.println(
                        rs.getString("stock_number") + " | " +
                        rs.getString("manufacturer_name") + " " +
                        rs.getString("model_number") + " | qty=" +
                        rs.getInt("quantity") + " | price=$" +
                        String.format("%.2f", rs.getDouble("price_each"))
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

    public void rerunOrder(int orderId) {
        String sql = """
            MERGE INTO emart_cart_items c
            USING (
                SELECT o.customer_id, oi.stock_number, oi.quantity
                FROM emart_orders o
                JOIN emart_order_items oi ON o.order_id = oi.order_id
                WHERE o.order_id = ?
            ) input
            ON (c.customer_id = input.customer_id AND c.stock_number = input.stock_number)
            WHEN MATCHED THEN
                UPDATE SET c.quantity = c.quantity + input.quantity
            WHEN NOT MATCHED THEN
                INSERT (customer_id, stock_number, quantity)
                VALUES (input.customer_id, input.stock_number, input.quantity)
        """;

        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orderId);
            int rows = stmt.executeUpdate();

            if (rows == 0) {
                System.out.println("Order not found or order had no items.");
            } else {
                System.out.println("Re-ran order #" + orderId + ".");
                System.out.println("Copied " + rows + " item(s) back into the cart.");
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

    private void updateCustomerStatus(Connection conn, String customerId) throws SQLException {
        String lastThreeSql = """
            SELECT NVL(SUM(subtotal), 0) AS recent_total
            FROM (
                SELECT subtotal
                FROM emart_orders
                WHERE customer_id = ?
                ORDER BY order_date DESC, order_id DESC
                FETCH FIRST 3 ROWS ONLY
            )
        """;

        double recentTotal;
        int orderCount;

        try (PreparedStatement stmt = conn.prepareStatement(lastThreeSql)) {
            stmt.setString(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                recentTotal = rs.getDouble("recent_total");
            }
        }

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) AS c FROM emart_orders WHERE customer_id = ?")) {
            stmt.setString(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                orderCount = rs.getInt("c");
            }
        }

        double goldThreshold = getRule(conn, "GOLD_STATUS_THRESHOLD");
        double silverThreshold = getRule(conn, "SILVER_STATUS_THRESHOLD");

        String newStatus;
        if (orderCount == 0) newStatus = "NEW";
        else if (recentTotal > goldThreshold) newStatus = "GOLD";
        else if (recentTotal > silverThreshold) newStatus = "SILVER";
        else if (recentTotal > 0) newStatus = "GREEN";
        else newStatus = "NEW";

        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE emart_customers SET status = ? WHERE customer_id = ?")) {
            stmt.setString(1, newStatus);
            stmt.setString(2, customerId);
            stmt.executeUpdate();
        }
    }
}
