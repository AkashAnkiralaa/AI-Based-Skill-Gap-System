package org.example.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DBUtil {
    private static final Properties props = new Properties();
    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        try (InputStream in = DBUtil.class.getResourceAsStream("/db.properties")) {
            props.load(in);
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Failed to load db.properties: " + e.getMessage());
        }
        URL = props.getProperty("jdbc.url");
        USER = props.getProperty("jdbc.user");
        PASSWORD = props.getProperty("jdbc.password");
        try {
            // Ensure Oracle driver is available
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            // driver not found - will fail later when attempting connection
        }
    }

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}

