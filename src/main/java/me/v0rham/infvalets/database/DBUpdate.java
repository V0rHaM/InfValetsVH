package me.v0rham.infvalets.database;

import me.v0rham.infvalets.Main;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBUpdate {

    public void createTable(String valetName) {
        try {
            PreparedStatement preparedStatement = Main.SQL.getConnection()
                    .prepareStatement("CREATE TABLE IF NOT EXISTS InfValets_" + valetName +
                            " (NAME VARCHAR(100), UUID VARCHAR(100)," +
                            " VALET_COUNT INT(100), PRIMARY KEY (UUID))");
            preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void createPlayerData(Player player, String valetName)  {
        try {
                PreparedStatement preparedStatement1 = Main.SQL.getConnection()
                        .prepareStatement("INSERT INTO InfValets_" + valetName
                                + " (NAME,UUID,VALET_COUNT) VALUES (?,?,?)");
                preparedStatement1.setString(1, player.getName());
                preparedStatement1.setString(2, player.getUniqueId().toString());
                preparedStatement1.setInt(3, 0);
                preparedStatement1.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void addMoney(String valetName, int money) {
        try {
            PreparedStatement preparedStatement = Main.SQL.getConnection()
                    .prepareStatement("UPDATE InfValet_" + valetName + " SET VALET_COUNT = VALET_COUNT + " + money);
            preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public int getBalance(String valetName) {
        try {
            PreparedStatement preparedStatement = Main.SQL.getConnection()
                    .prepareStatement("SELECT VALET_COUNT FROM InfValets_" + valetName + " WHERE UUID=? limit 1");
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) return Integer.parseInt(resultSet.toString());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return 0;
    }

    public void takeMoney(String valetName, int money) {
        try {
            PreparedStatement preparedStatement = Main.SQL.getConnection()
                    .prepareStatement("UPDATE InfValet_" + valetName + " SET VALET_COUNT = VALET_COUNT - " + money);
            preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }
}