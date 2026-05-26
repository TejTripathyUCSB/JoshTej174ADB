package org.yourcompany.yourproject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class InventoryService {
    
    private final ReplenishmentService replenishmentService;

    public InventoryService() {
        this.replenishmentService = new ReplenishmentService();
    }

    public int getInventoryQuantity(String stockNumber) throws SQLException {
        String sql = "SELECT quantity FROM item WHERE stock_number = ?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stockNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("quantity");
                }
            }
        }
        return -1; // Indicate item not found
    }

    public void fillOrder(int orderId) throws SQLException {
        Connection conn = null;
        try {
            conn = DB.getConnection();
            conn.setAutoCommit(false);

            // 1. Check if the order is PENDING
            String checkOrderSql = "SELECT fulfillment_status FROM emart_orders WHERE order_id = ?";
            try (PreparedStatement psCheckOrder = conn.prepareStatement(checkOrderSql)) {
                psCheckOrder.setInt(1, orderId);
                try (ResultSet rs = psCheckOrder.executeQuery()) {
                    if (rs.next()) {
                        String status = rs.getString("fulfillment_status");
                        if (!"PENDING".equals(status)) {
                            throw new IllegalStateException("Order " + orderId + " is not in PENDING status. Status: " + status);
                        }
                    } else {
                        throw new IllegalArgumentException("Order " + orderId + " does not exist.");
                    }
                }
            }

            // 2. Read order items and deduct quantities
            String getOrderItemsSql = "SELECT stock_number, quantity FROM emart_order_items WHERE order_id = ?";
            String checkItemSql = "SELECT quantity FROM item WHERE stock_number = ? FOR UPDATE";
            String updateItemSql = "UPDATE item SET quantity = quantity - ? WHERE stock_number = ?";

            try (PreparedStatement psGetItems = conn.prepareStatement(getOrderItemsSql);
                 PreparedStatement psCheckItem = conn.prepareStatement(checkItemSql);
                 PreparedStatement psUpdateItem = conn.prepareStatement(updateItemSql)) {
                 
                psGetItems.setInt(1, orderId);
                try (ResultSet rsItems = psGetItems.executeQuery()) {
                    while (rsItems.next()) {
                        String stockNumber = rsItems.getString("stock_number");
                        int orderedQty = rsItems.getInt("quantity");

                        psCheckItem.setString(1, stockNumber);
                        try (ResultSet rsItem = psCheckItem.executeQuery()) {
                            if (rsItem.next()) {
                                int availableQty = rsItem.getInt("quantity");
                                if (availableQty < orderedQty) {
                                    throw new IllegalStateException("Insufficient inventory for stock number: " + stockNumber + ". Needed: " + orderedQty + ", Available: " + availableQty);
                                }
                                
                                // Deduct quantity
                                psUpdateItem.setInt(1, orderedQty);
                                psUpdateItem.setString(2, stockNumber);
                                psUpdateItem.executeUpdate();
                            } else {
                                throw new IllegalArgumentException("Stock number " + stockNumber + " does not exist in inventory.");
                            }
                        }
                    }
                }
            }

            // 3. Update order status to FILLED
            String updateOrderSql = "UPDATE emart_orders SET fulfillment_status = 'FILLED', filled_date = SYSDATE WHERE order_id = ?";
            try (PreparedStatement psUpdateOrder = conn.prepareStatement(updateOrderSql)) {
                psUpdateOrder.setInt(1, orderId);
                psUpdateOrder.executeUpdate();
            }

            // 4. Trigger Replenishment Check using the same connection transaction
            replenishmentService.generateReplenishment(conn);

            // 5. Commit Transaction
            conn.commit();
        } catch (SQLException | IllegalStateException | IllegalArgumentException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.addSuppressed(e);
                    throw ex;
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }
}
