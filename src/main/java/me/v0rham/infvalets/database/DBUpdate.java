package me.v0rham.infvalets.database;

import me.v0rham.infvalets.Main;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBUpdate {

    public void createTable(String request) {
        try {
            PreparedStatement preparedStatement = Main.SQL.getConnection().prepareStatement(request);
            preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public static boolean exists(String tableName, String column, String condition) {
        try {
            PreparedStatement preparedStatement = Main.SQL.getConnection()
                    .prepareStatement("SELECT EXISTS ( SELECT " + column + " FROM " + tableName + " WHERE " + condition + " limit 1)");
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    public static void addData(String request)  {
        try {
                PreparedStatement preparedStatement1 = Main.SQL.getConnection()
                        .prepareStatement("INSERT INTO cooldown"
                                + " (NAME,UUID,TIMER) VALUES (?,?,?)");
                preparedStatement1.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public static void updateData(String tableName, String updateData, String condition) {
        try {
            PreparedStatement preparedStatement = Main.SQL.getConnection()
                    .prepareStatement("UPDATE " + tableName + " SET " + updateData + " WHERE " + condition);
            preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public static String getDataStr(String tableName, String column, String condition) {
        try {
            PreparedStatement preparedStatement = Main.SQL.getConnection()
                    .prepareStatement("SELECT " + column + " FROM " + tableName + " WHERE " + condition);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) return resultSet.toString();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return null;
    }
}