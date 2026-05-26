package org.yourcompany.yourproject;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DB {
    private static final Properties config = loadConfig();

    private static Properties loadConfig() {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream("db.properties")) {
            p.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Could not load db.properties", e);
        }
        return p;
    }

    public static Connection getConnection() throws SQLException {
        String url = "jdbc:oracle:thin:@" + config.getProperty("db.name")
                   + "?TNS_ADMIN=" + config.getProperty("wallet.path");
        Properties props = new Properties();
        props.put("user", config.getProperty("db.user"));
        props.put("password", config.getProperty("db.password"));
        return DriverManager.getConnection(url, props);
    }
}

