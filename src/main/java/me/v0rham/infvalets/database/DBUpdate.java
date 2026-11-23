package me.v0rham.infvalets.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Все методы принимают ТОЛЬКО имена валют, уже проверенные через Main#findValet
 * (имя таблицы нельзя передать параметром, поэтому оно склеивается в запрос).
 * <p>
 * SQL написан переносимо (без UPSERT), чтобы работать и на старых встроенных SQLite
 * (серверы 1.13+), и на PostgreSQL.
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
        Connection connection = db.getConnection();
        ensureRow(connection, uuid, name, valet);
        String sql = "UPDATE " + table(valet) + " SET VALET_COUNT = ? WHERE UUID = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, amount);
            ps.setString(2, uuid.toString());
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

    /** UUID игрока по последнему известному нику (без учёта регистра) или null. */
    public synchronized UUID findUuid(String name, Collection<String> valets) throws SQLException {
        for (String valet : valets) {
            String sql = "SELECT UUID FROM " + table(valet) + " WHERE LOWER(NAME) = LOWER(?)";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return UUID.fromString(rs.getString(1));
                }
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- конвертация валют

    // Отдельный префикс, чтобы имя таблицы не пересеклось с таблицей валюты
    private static final String HISTORY_TABLE = "InfValetsLog_conversions";

    public synchronized void createHistoryTable() throws SQLException {
        try (Statement statement = db.getConnection().createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + HISTORY_TABLE
                    + " (TS BIGINT NOT NULL, UUID VARCHAR(36) NOT NULL,"
                    + " FROM_VALET VARCHAR(32) NOT NULL, TO_VALET VARCHAR(32) NOT NULL,"
                    + " AMOUNT_IN BIGINT NOT NULL, AMOUNT_OUT BIGINT NOT NULL)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS InfValetsLog_conversions_idx ON "
                    + HISTORY_TABLE + " (TO_VALET, TS)");
        }
    }

    /** Удаляет записи истории конвертаций старше указанного времени. */
    public synchronized void pruneHistory(long beforeMillis) throws SQLException {
        String sql = "DELETE FROM " + HISTORY_TABLE + " WHERE TS < ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setLong(1, beforeMillis);
            ps.executeUpdate();
        }
    }

    /** Сколько конвертаций В эту валюту было с момента sinceMillis. */
    public synchronized long countConversions(String toValet, long sinceMillis) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + HISTORY_TABLE + " WHERE TO_VALET = ? AND TS >= ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, toValet);
            ps.setLong(2, sinceMillis);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    /** Общее количество валюты у всех игроков. */
    public synchronized long getTotal(String valet) throws SQLException {
        String sql = "SELECT COALESCE(SUM(VALET_COUNT), 0) FROM " + table(valet);
        try (Statement statement = db.getConnection().createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    /** Атомарный обмен: списываем amountIn валюты from, зачисляем amountOut валюты to, пишем в историю. */
    public synchronized TransferResult convert(UUID uuid, String name, String from, String to,
                                               long amountIn, long amountOut, long limitTo,
                                               long now) throws SQLException {
        Connection connection = db.getConnection();
        boolean oldAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            if (!take(uuid, from, amountIn)) {
                connection.rollback();
                return TransferResult.NOT_ENOUGH;
            }
            if (!add(uuid, name, to, amountOut, limitTo)) {
                connection.rollback();
                return TransferResult.LIMIT;
            }
            String sql = "INSERT INTO " + HISTORY_TABLE
                    + " (TS, UUID, FROM_VALET, TO_VALET, AMOUNT_IN, AMOUNT_OUT) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, now);
                ps.setString(2, uuid.toString());
                ps.setString(3, from);
                ps.setString(4, to);
                ps.setLong(5, amountIn);
                ps.setLong(6, amountOut);
                ps.executeUpdate();
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

    // Создаёт запись игрока (баланс 0), если её нет, и обновляет ник (если он известен)
    private void ensureRow(Connection connection, UUID uuid, String name, String valet) throws SQLException {
        String insert = db.isPostgres()
                ? "INSERT INTO " + table(valet) + " (UUID, NAME, VALET_COUNT) VALUES (?, ?, 0) ON CONFLICT (UUID) DO NOTHING"
                : "INSERT OR IGNORE INTO " + table(valet) + " (UUID, NAME, VALET_COUNT) VALUES (?, ?, 0)";
        try (PreparedStatement ps = connection.prepareStatement(insert)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.executeUpdate();
        }
        if (name != null) {
            String update = "UPDATE " + table(valet) + " SET NAME = ? WHERE UUID = ?";
            try (PreparedStatement ps = connection.prepareStatement(update)) {
                ps.setString(1, name);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            }
        }
    }

    private static String safeName(String name) {
        return name == null ? "?" : name;
    }
}
