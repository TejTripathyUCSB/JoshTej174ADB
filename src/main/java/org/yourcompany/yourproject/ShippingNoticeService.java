package org.yourcompany.yourproject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ShippingNoticeService {

    public void receiveShippingNotice(String noticeId, String manufacturer, String company, List<NoticeItem> items) throws SQLException {
        if (manufacturer == null || manufacturer.isBlank()) {
            throw new IllegalArgumentException("Notice header manufacturer is required.");
        }
        for (NoticeItem item : items) {
            String itemMfr = item.getManufacturer();
            if (itemMfr == null || !itemMfr.equalsIgnoreCase(manufacturer)) {
                throw new IllegalArgumentException(
                    "Notice item manufacturer '" + itemMfr +
                    "' does not match notice header manufacturer '" + manufacturer + "'"
                );
            }
        }

        Connection conn = null;
        try {
            conn = DB.getConnection();
            conn.setAutoCommit(false);

            String insertNoticeSql = "INSERT INTO edepot_shipping_notices (notice_id, manufacturer_name, shipping_company_name, status, notice_date) VALUES (?, ?, ?, 'PENDING', SYSDATE)";
            try (PreparedStatement ps = conn.prepareStatement(insertNoticeSql)) {
                ps.setString(1, noticeId);
                ps.setString(2, manufacturer);
                ps.setString(3, company);
                ps.executeUpdate();
            }

            String checkItemSql = "SELECT stock_number FROM item WHERE manufacturer_name = ? AND model_number = ?";
            String updateReplenishmentSql = "UPDATE item SET replenishment = replenishment + ? WHERE stock_number = ?";
            String insertItemSql = "INSERT INTO item (stock_number, manufacturer_name, model_number, quantity, min_stock, max_stock, location, replenishment) VALUES (?, ?, ?, 0, ?, ?, ?, ?)";
            String getSeqSql = "SELECT stock_number_seq.NEXTVAL FROM DUAL";
            
            String insertNoticeItemSql = "INSERT INTO edepot_shipping_notice_items (notice_id, stock_number, quantity) VALUES (?, ?, ?)";

            try (PreparedStatement psCheck = conn.prepareStatement(checkItemSql);
                 PreparedStatement psUpdateReplenishment = conn.prepareStatement(updateReplenishmentSql);
                 PreparedStatement psInsertItem = conn.prepareStatement(insertItemSql);
                 PreparedStatement psGetSeq = conn.prepareStatement(getSeqSql);
                 PreparedStatement psInsertNoticeItem = conn.prepareStatement(insertNoticeItemSql)) {

                for (NoticeItem item : items) {
                    psCheck.setString(1, item.getManufacturer());
                    psCheck.setString(2, item.getModelNumber());
                    String stockNumber = null;

                    try (ResultSet rs = psCheck.executeQuery()) {
                        if (rs.next()) {
                            stockNumber = rs.getString("stock_number");
                            // Exists: Update replenishment
                            psUpdateReplenishment.setInt(1, item.getQuantity());
                            psUpdateReplenishment.setString(2, stockNumber);
                            psUpdateReplenishment.executeUpdate();
                        }
                    }

                    if (stockNumber == null) {
                        int seqVal = 0;
                        try (ResultSet rsSeq = psGetSeq.executeQuery()) {
                            if (rsSeq.next()) {
                                seqVal = rsSeq.getInt(1);
                            }
                        }
                        stockNumber = String.format("ED%05d", seqVal);
                        String location = "W" + seqVal;

                        psInsertItem.setString(1, stockNumber);
                        psInsertItem.setString(2, item.getManufacturer());
                        psInsertItem.setString(3, item.getModelNumber());
                        psInsertItem.setInt(4, 10); // Default min_stock
                        psInsertItem.setInt(5, 100); // Default max_stock
                        psInsertItem.setString(6, location);
                        psInsertItem.setInt(7, item.getQuantity()); // initial replenishment
                        psInsertItem.executeUpdate();
                    }

                    psInsertNoticeItem.setString(1, noticeId);
                    psInsertNoticeItem.setString(2, stockNumber);
                    psInsertNoticeItem.setInt(3, item.getQuantity());
                    psInsertNoticeItem.executeUpdate();
                }
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    public void receiveShipment(String noticeId) throws SQLException {
        Connection conn = null;
        try {
            conn = DB.getConnection();
            conn.setAutoCommit(false);

            String noticeStatusSql = "SELECT status FROM edepot_shipping_notices WHERE notice_id = ? FOR UPDATE";
            try (PreparedStatement psStatus = conn.prepareStatement(noticeStatusSql)) {
                psStatus.setString(1, noticeId);
                try (ResultSet rs = psStatus.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Shipping notice not found: " + noticeId);
                    }
                    if ("RECEIVED".equals(rs.getString("status"))) {
                        throw new IllegalStateException("Shipping notice " + noticeId + " is already RECEIVED.");
                    }
                }
            }

            String getNoticeItemsSql = """
                SELECT ni.stock_number, ni.quantity AS shipped_qty,
                       i.quantity AS current_qty, i.max_stock, i.replenishment
                FROM edepot_shipping_notice_items ni
                JOIN item i ON ni.stock_number = i.stock_number
                WHERE ni.notice_id = ?
                FOR UPDATE
            """;
            
            String updateItemSql = "UPDATE item SET quantity = quantity + ?, replenishment = replenishment - ? WHERE stock_number = ?";

            try (PreparedStatement psGet = conn.prepareStatement(getNoticeItemsSql);
                 PreparedStatement psUpdate = conn.prepareStatement(updateItemSql)) {

                psGet.setString(1, noticeId);
                try (ResultSet rs = psGet.executeQuery()) {
                    boolean foundItems = false;
                    while (rs.next()) {
                        foundItems = true;
                        String stockNumber = rs.getString("stock_number");
                        int shippedQty = rs.getInt("shipped_qty");
                        int currentQty = rs.getInt("current_qty");
                        int maxStock = rs.getInt("max_stock");
                        int currentReplenishment = rs.getInt("replenishment");

                        if (currentQty + shippedQty > maxStock) {
                            throw new IllegalStateException(
                                "Receiving notice " + noticeId + " would exceed max_stock for " + stockNumber
                            );
                        }
                        if (currentReplenishment < shippedQty) {
                            throw new IllegalStateException(
                                "Replenishment underflow for " + stockNumber + ": shipped " + shippedQty +
                                " but replenishment is " + currentReplenishment
                            );
                        }

                        psUpdate.setInt(1, shippedQty);
                        psUpdate.setInt(2, shippedQty);
                        psUpdate.setString(3, stockNumber);
                        psUpdate.executeUpdate();
                    }
                    if (!foundItems) {
                        throw new IllegalArgumentException("Shipping notice has no line items: " + noticeId);
                    }
                }
            }

            String updateNoticeSql = "UPDATE edepot_shipping_notices SET status = 'RECEIVED', received_date = SYSDATE WHERE notice_id = ?";
            try (PreparedStatement psNotice = conn.prepareStatement(updateNoticeSql)) {
                psNotice.setString(1, noticeId);
                psNotice.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
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