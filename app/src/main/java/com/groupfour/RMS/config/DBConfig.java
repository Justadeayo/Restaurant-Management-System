package com.groupfour.RMS.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public class DBConfig {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/restaurant_management";

    private static String url;
    private static String username;
    private static String password;

    static {
        loadProperties();
    }

    private static void loadProperties() {
        Properties props = new Properties();
        Path configPath = Paths.get(System.getProperty("user.dir"))
                .getParent()
                .resolve("config.properties");
        System.out.println("ConfigPath: " + configPath);
        try (InputStream input = Files.newInputStream(configPath)) {
            props.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("config.properties not found at " + configPath
                    + ". Copy config.properties.example to config.properties and set your database details.", e);
        }
        url = props.getProperty("db.url", DEFAULT_URL);
        username = props.getProperty("db.user");
        password = props.getProperty("db.password");
        if (username == null || password == null) {
            throw new IllegalStateException("config.properties must set db.user and db.password.");
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    public static void reload() {
        loadProperties();
    }
}