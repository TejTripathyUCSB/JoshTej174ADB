package org.yourcompany.yourproject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {

    public static final class Session {
        public final String customerId;
        public final boolean manager;

        public Session(String customerId, boolean manager) {
            this.customerId = customerId;
            this.manager = manager;
        }
    }

    public static final class AuthException extends RuntimeException {
        public AuthException(String msg, Throwable cause) { super(msg, cause); }
    }

    public Session login(String customerId, String password) {
        if (customerId == null || customerId.isEmpty() || password == null) {
            return null;
        }
        String sql = "SELECT is_manager FROM emart_customers " +
                     "WHERE customer_id = ? AND password = ?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String flag = rs.getString("is_manager");
                boolean isManager = flag != null &&
                    (flag.equalsIgnoreCase("T") ||
                     flag.equalsIgnoreCase("Y") ||
                     flag.equals("1") ||
                     flag.equalsIgnoreCase("TRUE"));
                return new Session(customerId, isManager);
            }
        } catch (SQLException e) {
            throw new AuthException(
                "Login query failed (is the is_manager column present? " +
                "run sql/04_add_login.sql): " + e.getMessage(), e);
        }
    }
}
