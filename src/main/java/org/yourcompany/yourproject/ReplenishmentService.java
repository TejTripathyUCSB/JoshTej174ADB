package org.yourcompany.yourproject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReplenishmentService {
    private static final class ReplenishmentLine {
        private final String stockNumber;
        private final int amountToOrder;

        private ReplenishmentLine(String stockNumber, int amountToOrder) {
            this.stockNumber = stockNumber;
            this.amountToOrder = amountToOrder;
        }
    }

    public void generateReplenishment(Connection conn) throws SQLException {
        String findMfrSql = "SELECT manufacturer_name FROM item WHERE quantity < min_stock GROUP BY manufacturer_name HAVING COUNT(*) >= 3";
        String getSeqSql = "SELECT replenishment_order_seq.NEXTVAL FROM DUAL";
        String insertOrderSql = "INSERT INTO edepot_replenishment_orders (order_id, manufacturer_name, order_date) VALUES (?, ?, SYSDATE)";
        String getItemsSql = "SELECT stock_number, quantity, replenishment, max_stock FROM item WHERE manufacturer_name = ? AND quantity < max_stock";
        String insertItemSql = "INSERT INTO edepot_replenishment_items (order_id, stock_number, quantity) VALUES (?, ?, ?)";

        try (PreparedStatement psFindMfr = conn.prepareStatement(findMfrSql);
             PreparedStatement psGetSeq = conn.prepareStatement(getSeqSql);
             PreparedStatement psInsertOrder = conn.prepareStatement(insertOrderSql);
             PreparedStatement psGetItems = conn.prepareStatement(getItemsSql);
             PreparedStatement psInsertItem = conn.prepareStatement(insertItemSql)) {

            List<String> manufacturers = new ArrayList<>();
            try (ResultSet rs = psFindMfr.executeQuery()) {
                while (rs.next()) {
                    manufacturers.add(rs.getString("manufacturer_name"));
                }
            }

            for (String manufacturer : manufacturers) {
                List<ReplenishmentLine> lines = new ArrayList<>();
                psGetItems.setString(1, manufacturer);
                try (ResultSet rsItems = psGetItems.executeQuery()) {
                    while (rsItems.next()) {
                        String stockNumber = rsItems.getString("stock_number");
                        int quantity = rsItems.getInt("quantity");
                        int replenishment = rsItems.getInt("replenishment");
                        int maxStock = rsItems.getInt("max_stock");

                        int amountToOrder = maxStock - (quantity + replenishment);
                        if (amountToOrder > 0) {
                            lines.add(new ReplenishmentLine(stockNumber, amountToOrder));
                        }
                    }
                }

                if (lines.isEmpty()) {
                    continue;
                }

                String orderId = null;
                try (ResultSet rsSeq = psGetSeq.executeQuery()) {
                    if (rsSeq.next()) {
                        orderId = String.format("RO%05d", rsSeq.getInt(1));
                    }
                }

                if (orderId == null) {
                    throw new SQLException("Could not create replenishment order ID.");
                }

                psInsertOrder.setString(1, orderId);
                psInsertOrder.setString(2, manufacturer);
                psInsertOrder.executeUpdate();

                for (ReplenishmentLine line : lines) {
                    psInsertItem.setString(1, orderId);
                    psInsertItem.setString(2, line.stockNumber);
                    psInsertItem.setInt(3, line.amountToOrder);
                    psInsertItem.executeUpdate();
                }
            }
        }
    }
}
