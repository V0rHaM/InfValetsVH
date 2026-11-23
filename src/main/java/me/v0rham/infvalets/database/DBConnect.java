package me.v0rham.infvalets.database;

import me.v0rham.infvalets.Main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnect {

    private final Main plugin;
    private Connection connection;

    public DBConnect(Main plugin) {
        this.plugin = plugin;
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException exception) {
            return false;
        }
    }

    public void connect() throws SQLException {
        if (isConnected()) return;

        String url = plugin.getConfig().getString("Database.url", "jdbc:sqlite:plugins/InfValets/data.db");
        loadDriver(url);
        connection = DriverManager.getConnection(url);
    }

    // Драйвер из "libraries:" лежит в classloader плагина, поэтому регистрируем его вручную
    private void loadDriver(String url) throws SQLException {
        String driver = null;
        if (url.startsWith("jdbc:sqlite:")) driver = "org.sqlite.JDBC";
        else if (url.startsWith("jdbc:postgresql:")) driver = "org.postgresql.Driver";
        if (driver == null) return;
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException exception) {
            throw new SQLException("JDBC driver not found: " + driver, exception);
        }
    }

    public void disconnect() {
        if (connection == null) return;
        try {
            connection.close();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Failed to close database: " + exception.getMessage());
        }
        connection = null;
    }

    /** Возвращает живое соединение, при необходимости переподключается. */
    public Connection getConnection() throws SQLException {
        connect();
        return connection;
    }
}
