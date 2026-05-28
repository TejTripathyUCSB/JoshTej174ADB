package org.yourcompany.yourproject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ManagerService {

    public record ManufacturerOrderLine(String stockNumber, int quantity) {}

    public void changePrice(String stockNumber, double newPrice) {
        String sql = """
            UPDATE emart_products
            SET price = ?
            WHERE stock_number = ?
        """;

        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newPrice);
            stmt.setString(2, stockNumber);

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                System.out.printf("Price changed for %s to $%.2f%n", stockNumber, newPrice);
            } else {
                System.out.println("Product not found.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printMonthlySalesReport(int year, int month) {
        System.out.println("\n=== Monthly Sales Report: " + year + "-" + String.format("%02d", month) + " ===");

        printSalesByProduct(year, month);
        printSalesByCategory(year, month);
        printTopCustomer(year, month);
    }

    private void printSalesByProduct(int year, int month) {
        String sql = """
            SELECT oi.stock_number,
                   i.manufacturer_name,
                   i.model_number,
                   SUM(oi.quantity) AS total_quantity,
                   SUM(oi.quantity * oi.price_each) AS total_sales
            FROM emart_orders o
            JOIN emart_order_items oi ON o.order_id = oi.order_id
            JOIN item i ON oi.stock_number = i.stock_number
            WHERE EXTRACT(YEAR FROM o.order_date) = ?
              AND EXTRACT(MONTH FROM o.order_date) = ?
            GROUP BY oi.stock_number, i.manufacturer_name, i.model_number
            ORDER BY total_sales DESC
        """;

        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            try (ResultSet rs = stmt.executeQuery()) {
                System.out.println("\nSales by product:");

                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf(
                        "%s | %s %s | qty=%d | sales=$%.2f%n",
                        rs.getString("stock_number"),
                        rs.getString("manufacturer_name"),
                        rs.getString("model_number"),
                        rs.getInt("total_quantity"),
                        rs.getDouble("total_sales")
                    );
                }

                if (!found) {
                    System.out.println("No product sales for this month.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printSalesByCategory(int year, int month) {
        String sql = """
            SELECT p.category,
                   SUM(oi.quantity) AS total_quantity,
                   SUM(oi.quantity * oi.price_each) AS total_sales
            FROM emart_orders o
            JOIN emart_order_items oi ON o.order_id = oi.order_id
            JOIN emart_products p ON oi.stock_number = p.stock_number
            WHERE EXTRACT(YEAR FROM o.order_date) = ?
              AND EXTRACT(MONTH FROM o.order_date) = ?
            GROUP BY p.category
            ORDER BY total_sales DESC
        """;

        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            try (ResultSet rs = stmt.executeQuery()) {
                System.out.println("\nSales by category:");

                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf(
                        "%s | qty=%d | sales=$%.2f%n",
                        rs.getString("category"),
                        rs.getInt("total_quantity"),
                        rs.getDouble("total_sales")
                    );
                }

                if (!found) {
                    System.out.println("No category sales for this month.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printTopCustomer(int year, int month) {
        String sql = """
            SELECT o.customer_id,
                   c.first_name,
                   c.middle_name,
                   c.last_name,
                   SUM(o.total) AS total_spent
            FROM emart_orders o
            JOIN emart_customers c ON o.customer_id = c.customer_id
            WHERE EXTRACT(YEAR FROM o.order_date) = ?
              AND EXTRACT(MONTH FROM o.order_date) = ?
            GROUP BY o.customer_id, c.first_name, c.middle_name, c.last_name
            ORDER BY total_spent DESC
            FETCH FIRST 1 ROWS ONLY
        """;

        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            try (ResultSet rs = stmt.executeQuery()) {
                System.out.println("\nTop customer:");

                if (rs.next()) {
                    String middle = rs.getString("middle_name");
                    String fullName = rs.getString("first_name") + " " +
                            (middle == null ? "" : middle + " ") +
                            rs.getString("last_name");

                    System.out.printf(
                        "%s | %s | spent=$%.2f%n",
                        rs.getString("customer_id"),
                        fullName,
                        rs.getDouble("total_spent")
                    );
                } else {
                    System.out.println("No customer purchases for this month.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateCustomerStatusManually(String customerId, String newStatus) {
        String sql = """
            UPDATE emart_customers
            SET status = ?
            WHERE customer_id = ?
        """;

        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus.toUpperCase());
            stmt.setString(2, customerId);

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                System.out.println("Customer " + customerId + " status changed to " + newStatus.toUpperCase());
            } else {
                System.out.println("Customer not found.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendManufacturerOrder(String manufacturer, List<ManufacturerOrderLine> lines) throws SQLException {
        if (manufacturer == null || manufacturer.isBlank()) {
            throw new IllegalArgumentException("Manufacturer is required.");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Order must include at least one line.");
        }
        for (ManufacturerOrderLine line : lines) {
            if (line.stockNumber() == null || line.stockNumber().isBlank()) {
                throw new IllegalArgumentException("Stock number is required on every line.");
            }
            if (line.quantity() <= 0) {
                throw new IllegalArgumentException("Line quantity must be > 0 for " + line.stockNumber());
            }
        }

        Connection conn = null;
        try {
            conn = DB.getConnection();
            conn.setAutoCommit(false);

            String validateSql = "SELECT manufacturer_name FROM item WHERE stock_number = ?";
            try (PreparedStatement ps = conn.prepareStatement(validateSql)) {
                for (ManufacturerOrderLine line : lines) {
                    ps.setString(1, line.stockNumber());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                "Stock number not found: " + line.stockNumber()
                            );
                        }
                        String actualMfr = rs.getString("manufacturer_name");
                        if (!actualMfr.equalsIgnoreCase(manufacturer)) {
                            throw new IllegalArgumentException(
                                "Stock " + line.stockNumber() + " belongs to " + actualMfr +
                                ", not " + manufacturer
                            );
                        }
                    }
                }
            }

            String orderId;
            try (PreparedStatement ps = conn.prepareStatement("SELECT replenishment_order_seq.NEXTVAL FROM DUAL");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                orderId = String.format("RO%05d", rs.getInt(1));
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO edepot_replenishment_orders (order_id, manufacturer_name, order_date) VALUES (?, ?, SYSDATE)")) {
                ps.setString(1, orderId);
                ps.setString(2, manufacturer);
                ps.executeUpdate();
            }

            String insertItemSql = "INSERT INTO edepot_replenishment_items (order_id, stock_number, quantity) VALUES (?, ?, ?)";
            try (PreparedStatement psItem = conn.prepareStatement(insertItemSql)) {
                for (ManufacturerOrderLine line : lines) {
                    psItem.setString(1, orderId);
                    psItem.setString(2, line.stockNumber());
                    psItem.setInt(3, line.quantity());
                    psItem.executeUpdate();
                }
            }

            conn.commit();
            System.out.println("Sent manufacturer order " + orderId + " to " + manufacturer +
                               " (" + lines.size() + " line(s)).");
            for (ManufacturerOrderLine line : lines) {
                System.out.println("  " + line.stockNumber() + " x " + line.quantity());
            }
        } catch (SQLException | IllegalArgumentException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    public void deleteOldSalesTransactions() {
        String sql = """
            DELETE FROM emart_orders
            WHERE order_id IN (
                SELECT order_id
                FROM (
                    SELECT order_id,
                           ROW_NUMBER() OVER (
                               PARTITION BY customer_id
                               ORDER BY order_date DESC, order_id DESC
                           ) AS rn
                    FROM emart_orders
                )
                WHERE rn > 3
            )
        """;

        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int rows = stmt.executeUpdate();
            System.out.println("Deleted " + rows + " old order(s).");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}