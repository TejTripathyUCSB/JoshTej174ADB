package org.yourcompany.yourproject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DB {
    private static final String WALLET_PATH = "/Users/tejtripathy/Downloads/Wallet_JoshTej174A";
    private static final String DB_NAME = "joshtej174a_tp";
    private static final String DB_USER = "ADMIN";
    private static final String DB_PASSWORD = "Neeta87ms$$$";

    public static Connection getConnection() throws SQLException {
        String jdbcUrl = "jdbc:oracle:thin:@" + DB_NAME + "?TNS_ADMIN=" + WALLET_PATH;

        Properties props = new Properties();
        props.put("user", DB_USER);
        props.put("password", DB_PASSWORD);

        return DriverManager.getConnection(jdbcUrl, props);
    }
}