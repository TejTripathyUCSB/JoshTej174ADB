package org.yourcompany.yourproject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReplenishmentService {

    public void generateReplenishment(Connection conn) throws SQLException {
        // Find manufacturers that have >= 3 items where quantity < min_stock
        String findMfrSql = "SELECT manufacturer_name FROM item WHERE quantity < min_stock GROUP BY manufacturer_name HAVING COUNT(*) >= 3";
        
        // For these manufacturers, insert into edepot_replenishment_orders
        String getSeqSql = "SELECT replenishment_order_seq.NEXTVAL FROM DUAL";
        String insertOrderSql = "INSERT INTO edepot_replenishment_orders (order_id, manufacturer_name, order_date) VALUES (?, ?, SYSDATE)";
        
        // Then, insert all items for that manufacturer where quantity < max_stock
        String getItemsSql = "SELECT stock_number, quantity, replenishment, max_stock FROM item WHERE manufacturer_name = ? AND quantity < max_stock";
        String insertItemSql = "INSERT INTO edepot_replenishment_items (order_id, stock_number, quantity) VALUES (?, ?, ?)";
        String updateReplenishmentSql = "UPDATE item SET replenishment = replenishment + ? WHERE stock_number = ?";

        try (PreparedStatement psFindMfr = conn.prepareStatement(findMfrSql);
             PreparedStatement psGetSeq = conn.prepareStatement(getSeqSql);
             PreparedStatement psInsertOrder = conn.prepareStatement(insertOrderSql);
             PreparedStatement psGetItems = conn.prepareStatement(getItemsSql);
             PreparedStatement psInsertItem = conn.prepareStatement(insertItemSql);
             PreparedStatement psUpdateReplenish = conn.prepareStatement(updateReplenishmentSql)) {

            List<String> manufacturers = new ArrayList<>();
            try (ResultSet rs = psFindMfr.executeQuery()) {
                while (rs.next()) {
                    manufacturers.add(rs.getString("manufacturer_name"));
                }
            }

            for (String manufacturer : manufacturers) {
                // Generate Order ID
                String orderId = null;
                try (ResultSet rsSeq = psGetSeq.executeQuery()) {
                    if (rsSeq.next()) {
                        orderId = "RO" + rsSeq.getInt(1);
                    }
                }

                // Create Order
                psInsertOrder.setString(1, orderId);
                psInsertOrder.setString(2, manufacturer);
                psInsertOrder.executeUpdate();

                // Add Items to Order
                psGetItems.setString(1, manufacturer);
                try (ResultSet rsItems = psGetItems.executeQuery()) {
                    while (rsItems.next()) {
                        String stockNumber = rsItems.getString("stock_number");
                        int quantity = rsItems.getInt("quantity");
                        int replenishment = rsItems.getInt("replenishment");
                        int maxStock = rsItems.getInt("max_stock");

                        // Calculate amount needed to reach max_stock
                        int amountToOrder = maxStock - (quantity + replenishment);
                        if (amountToOrder > 0) {
                            // Insert into edepot_replenishment_items
                            psInsertItem.setString(1, orderId);
                            psInsertItem.setString(2, stockNumber);
                            psInsertItem.setInt(3, amountToOrder);
                            psInsertItem.executeUpdate();

                            // Crucial: Update the replenishment amount in item table immediately
                            psUpdateReplenish.setInt(1, amountToOrder);
                            psUpdateReplenish.setString(2, stockNumber);
                            psUpdateReplenish.executeUpdate();
                        }
                    }
                }
            }
        }
    }
}
