package me.v0rham.infvalets.database;

import me.v0rham.infvalets.Main;

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
            connection = DriverManager.getConnection(
                    Main.getInstance().getConfig().
                            getString("Database.url", "jdbc:sqlite:plugins/InfValets/data.db"));
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
