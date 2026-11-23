package me.v0rham.infvalets.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Все методы принимают ТОЛЬКО имена валют, уже проверенные через Main#findValet
 * (имя таблицы нельзя передать параметром, поэтому оно склеивается в запрос).
 */
public class DBUpdate {

    public enum TransferResult { OK, NOT_ENOUGH, LIMIT }

    private static final String TABLE_PREFIX = "InfValets_";

    private final DBConnect db;

    public DBUpdate(DBConnect db) {
        this.db = db;
    }

    private static String table(String valet) {
        return TABLE_PREFIX + valet;
    }

    public synchronized void createTable(String valet) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + table(valet)
                + " (UUID VARCHAR(36) NOT NULL PRIMARY KEY,"
                + " NAME VARCHAR(16),"
                + " VALET_COUNT BIGINT NOT NULL DEFAULT 0)";
        try (Statement statement = db.getConnection().createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    /** Баланс игрока; если записи ещё нет — 0. */
    public synchronized long getBalance(UUID uuid, String valet) throws SQLException {
        String sql = "SELECT VALET_COUNT FROM " + table(valet) + " WHERE UUID = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    /** Добавляет amount, если итог не превысит limit. false — лимит. */
    public synchronized boolean add(UUID uuid, String name, String valet, long amount, long limit) throws SQLException {
        if (amount > limit) return false;
        Connection connection = db.getConnection();
        ensureRow(connection, uuid, name, valet);
        String sql = "UPDATE " + table(valet) + " SET VALET_COUNT = VALET_COUNT + ?"
                + " WHERE UUID = ? AND VALET_COUNT + ? <= ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, amount);
            ps.setString(2, uuid.toString());
            ps.setLong(3, amount);
            ps.setLong(4, limit);
            return ps.executeUpdate() == 1;
        }
    }

    /** Списывает amount, если хватает средств. false — не хватает. */
    public synchronized boolean take(UUID uuid, String valet, long amount) throws SQLException {
        String sql = "UPDATE " + table(valet) + " SET VALET_COUNT = VALET_COUNT - ?"
                + " WHERE UUID = ? AND VALET_COUNT >= ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setLong(1, amount);
            ps.setString(2, uuid.toString());
            ps.setLong(3, amount);
            return ps.executeUpdate() == 1;
        }
    }

    public synchronized void set(UUID uuid, String name, String valet, long amount) throws SQLException {
        String update = name == null
                ? "VALET_COUNT = excluded.VALET_COUNT"
                : "NAME = excluded.NAME, VALET_COUNT = excluded.VALET_COUNT";
        String sql = "INSERT INTO " + table(valet) + " (UUID, NAME, VALET_COUNT) VALUES (?, ?, ?)"
                + " ON CONFLICT (UUID) DO UPDATE SET " + update;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setLong(3, amount);
            ps.executeUpdate();
        }
    }

    /** Атомарный перевод: либо списали и зачислили, либо ничего не изменилось. */
    public synchronized TransferResult transfer(UUID from, UUID to, String toName, String valet,
                                                long amount, long limit) throws SQLException {
        Connection connection = db.getConnection();
        boolean oldAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            if (!take(from, valet, amount)) {
                connection.rollback();
                return TransferResult.NOT_ENOUGH;
            }
            if (!add(to, toName, valet, amount, limit)) {
                connection.rollback();
                return TransferResult.LIMIT;
            }
            connection.commit();
            return TransferResult.OK;
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(oldAutoCommit);
        }
    }

    /** Список (ник, баланс) по убыванию. */
    public synchronized List<Map.Entry<String, Long>> top(String valet, int size) throws SQLException {
        String sql = "SELECT NAME, VALET_COUNT FROM " + table(valet)
                + " ORDER BY VALET_COUNT DESC LIMIT ?";
        List<Map.Entry<String, Long>> result = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, size);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new AbstractMap.SimpleEntry<>(safeName(rs.getString(1)), rs.getLong(2)));
                }
            }
        }
        return result;
    }

    // Создаёт запись игрока (баланс 0) или обновляет ник
    private void ensureRow(Connection connection, UUID uuid, String name, String valet) throws SQLException {
        String action = name == null ? "DO NOTHING" : "DO UPDATE SET NAME = excluded.NAME";
        String sql = "INSERT INTO " + table(valet) + " (UUID, NAME, VALET_COUNT) VALUES (?, ?, 0)"
                + " ON CONFLICT (UUID) " + action;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.executeUpdate();
        }
    }

    private static String safeName(String name) {
        return name == null ? "?" : name;
    }
}
