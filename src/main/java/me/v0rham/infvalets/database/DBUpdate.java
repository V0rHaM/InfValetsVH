package me.v0rham.infvalets.database;

import me.v0rham.infvalets.Main;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class DBUpdate {

    public static void createTable(String valetName) {
        try {
            PreparedStatement preparedStatement = Main.SQL.getConnection()
                    .prepareStatement("CREATE TABLE IF NOT EXISTS InfValets_" + valetName +
                            " (NAME VARCHAR(100), UUID VARCHAR(100), VALET_NAME VARCHAR(100)" +
                            "VALET_COUNT INT, PRIMARY KEY (UUID))");
            preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public static boolean existsPlayer(UUID uuid) {
        try {
            PreparedStatement preparedStatement = Main.SQL.getConnection()
                    .prepareStatement("SELECT EXISTS ( SELECT " + uuid + " FROM InfValets WHERE UUID=? limit 1)");
            preparedStatement.setString(1, uuid.toString());
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
                        .prepareStatement("INSERT INTO InfValet"
                                + " (NAME,UUID,VALET_NAME) VALUES (?,?,?)");
                preparedStatement1.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public static void addMoney(String valetName, int money) {
        try {
            PreparedStatement preparedStatement = Main.SQL.getConnection()
                    .prepareStatement("UPDATE InfValet_" + valetName + " SET VALET_VALUE = VALET_VALUE" + money);
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