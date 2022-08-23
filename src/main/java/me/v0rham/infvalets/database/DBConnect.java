package me.v0rham.infvalets.database;

import me.v0rham.infvalets.Main;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnect {

    private Connection connection;

    public boolean isConnected() {
        return (connection != null);
    }

    public void connect() throws SQLException {
        if (!isConnected()) {
            connection = DriverManager.getConnection(Main.getInstance()
                    .getConfig().getString("Settings.database-string",
                    "jdbc:sqlite:" + Main.getInstance().getDataFolder().getName() + "/data.db"));
        }
    }

    public void disconnect() {
        if (isConnected()) {
            try {
                connection.close();
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
