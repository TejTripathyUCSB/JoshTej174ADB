package org.yourcompany.yourproject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InventoryService {

    private static final class OrderLine {
        private final String stockNumber;
        private final int quantity;

        private OrderLine(String stockNumber, int quantity) {
            this.stockNumber = stockNumber;
            this.quantity = quantity;
        }
    }

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

            String checkOrderSql = "SELECT fulfillment_status FROM emart_orders WHERE order_id = ?";
            try (PreparedStatement psCheckOrder = conn.prepareStatement(checkOrderSql)) {
                psCheckOrder.setInt(1, orderId);
                try (ResultSet rs = psCheckOrder.executeQuery()) {
                    if (rs.next()) {
                        String status = rs.getString("fulfillment_status");
                        if ("FILLED".equals(status)) {
                            throw new IllegalStateException("Order " + orderId + " is already FILLED.");
                        }
                        if (!"PENDING".equals(status)) {
                            throw new IllegalStateException("Order " + orderId + " is not fillable. Status: " + status);
                        }
                    } else {
                        throw new IllegalArgumentException("Order " + orderId + " does not exist.");
                    }
                }
            }

            String getOrderItemsSql = "SELECT stock_number, quantity FROM emart_order_items WHERE order_id = ?";
            String checkItemSql = "SELECT quantity FROM item WHERE stock_number = ? FOR UPDATE";
            String updateItemSql = "UPDATE item SET quantity = quantity - ? WHERE stock_number = ?";

            List<OrderLine> lines = new ArrayList<>();
            try (PreparedStatement psGetItems = conn.prepareStatement(getOrderItemsSql);
                 PreparedStatement psCheckItem = conn.prepareStatement(checkItemSql);
                 PreparedStatement psUpdateItem = conn.prepareStatement(updateItemSql)) {

                psGetItems.setInt(1, orderId);
                try (ResultSet rsItems = psGetItems.executeQuery()) {
                    while (rsItems.next()) {
                        lines.add(new OrderLine(rsItems.getString("stock_number"), rsItems.getInt("quantity")));
                    }
                }

                if (lines.isEmpty()) {
                    throw new IllegalStateException("Order " + orderId + " has no items.");
                }

                for (OrderLine line : lines) {
                    psCheckItem.setString(1, line.stockNumber);
                    try (ResultSet rsItem = psCheckItem.executeQuery()) {
                        if (rsItem.next()) {
                            int availableQty = rsItem.getInt("quantity");
                            if (availableQty < line.quantity) {
                                throw new IllegalStateException(
                                    "Insufficient inventory for " + line.stockNumber +
                                    ". Needed: " + line.quantity + ", Available: " + availableQty
                                );
                            }
                        } else {
                            throw new IllegalArgumentException("Stock number not found in item: " + line.stockNumber);
                        }
                    }
                }

                for (OrderLine line : lines) {
                    psUpdateItem.setInt(1, line.quantity);
                    psUpdateItem.setString(2, line.stockNumber);
                    psUpdateItem.executeUpdate();
                }
            }

            String updateOrderSql = "UPDATE emart_orders SET fulfillment_status = 'FILLED', filled_date = SYSDATE WHERE order_id = ?";
            try (PreparedStatement psUpdateOrder = conn.prepareStatement(updateOrderSql)) {
                psUpdateOrder.setInt(1, orderId);
                psUpdateOrder.executeUpdate();
            }

            replenishmentService.generateReplenishment(conn);
            conn.commit();
            System.out.println("Filled order #" + orderId + ".");
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
