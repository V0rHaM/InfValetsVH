package me.v0rham.infvalets.api;

import me.v0rham.infvalets.Main;
import me.v0rham.infvalets.database.DBUpdate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Реализация {@link InfValetsAPI}. Регистрируется в ServicesManager в Main#onEnable. */
public class InfValetsAPIImpl implements InfValetsAPI {

    private final Main plugin;

    public InfValetsAPIImpl(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public Collection<String> getValets() {
        return plugin.getValets();
    }

    @Override
    public String findValet(String name) {
        return plugin.findValet(name);
    }

    @Override
    public long getLimit(String valet) {
        String resolved = plugin.findValet(valet);
        return resolved == null ? -1 : plugin.getLimit(resolved);
    }

    @Override
    public long getBalance(UUID uuid, String valet) {
        String resolved = plugin.findValet(valet);
        if (resolved == null || !plugin.isEnabled()) return -1;
        try {
            return data().getBalance(uuid, resolved);
        } catch (SQLException exception) {
            log(exception);
            return -1;
        }
    }

    @Override
    public boolean has(UUID uuid, String valet, long amount) {
        return amount >= 0 && getBalance(uuid, valet) >= amount;
    }

    @Override
    public TransactionResult deposit(UUID uuid, String playerName, String valet, long amount) {
        String resolved = plugin.findValet(valet);
        if (resolved == null) return TransactionResult.UNKNOWN_VALET;
        if (amount <= 0) return TransactionResult.INVALID_AMOUNT;
        if (!plugin.isEnabled()) return TransactionResult.ERROR;
        try {
            return data().add(uuid, playerName, resolved, amount, plugin.getLimit(resolved))
                    ? TransactionResult.SUCCESS : TransactionResult.LIMIT_REACHED;
        } catch (SQLException exception) {
            log(exception);
            return TransactionResult.ERROR;
        }
    }

    @Override
    public TransactionResult withdraw(UUID uuid, String valet, long amount) {
        String resolved = plugin.findValet(valet);
        if (resolved == null) return TransactionResult.UNKNOWN_VALET;
        if (amount <= 0) return TransactionResult.INVALID_AMOUNT;
        if (!plugin.isEnabled()) return TransactionResult.ERROR;
        try {
            return data().take(uuid, resolved, amount)
                    ? TransactionResult.SUCCESS : TransactionResult.INSUFFICIENT_FUNDS;
        } catch (SQLException exception) {
            log(exception);
            return TransactionResult.ERROR;
        }
    }

    @Override
    public TransactionResult set(UUID uuid, String playerName, String valet, long amount) {
        String resolved = plugin.findValet(valet);
        if (resolved == null) return TransactionResult.UNKNOWN_VALET;
        if (amount < 0) return TransactionResult.INVALID_AMOUNT;
        if (amount > plugin.getLimit(resolved)) return TransactionResult.LIMIT_REACHED;
        if (!plugin.isEnabled()) return TransactionResult.ERROR;
        try {
            data().set(uuid, playerName, resolved, amount);
            return TransactionResult.SUCCESS;
        } catch (SQLException exception) {
            log(exception);
            return TransactionResult.ERROR;
        }
    }

    @Override
    public TransactionResult transfer(UUID from, UUID to, String toName, String valet, long amount) {
        String resolved = plugin.findValet(valet);
        if (resolved == null) return TransactionResult.UNKNOWN_VALET;
        if (amount <= 0) return TransactionResult.INVALID_AMOUNT;
        if (from.equals(to)) return TransactionResult.SAME_ACCOUNT;
        if (!plugin.isEnabled()) return TransactionResult.ERROR;
        try {
            switch (data().transfer(from, to, toName, resolved, amount, plugin.getLimit(resolved))) {
                case OK:
                    return TransactionResult.SUCCESS;
                case NOT_ENOUGH:
                    return TransactionResult.INSUFFICIENT_FUNDS;
                default:
                    return TransactionResult.LIMIT_REACHED;
            }
        } catch (SQLException exception) {
            log(exception);
            return TransactionResult.ERROR;
        }
    }

    @Override
    public List<TopEntry> getTop(String valet, int size) {
        String resolved = plugin.findValet(valet);
        if (resolved == null || size <= 0 || !plugin.isEnabled()) return Collections.emptyList();
        try {
            List<TopEntry> result = new ArrayList<>();
            for (Map.Entry<String, Long> entry : data().top(resolved, size)) {
                result.add(new TopEntry(entry.getKey(), entry.getValue()));
            }
            return result;
        } catch (SQLException exception) {
            log(exception);
            return Collections.emptyList();
        }
    }

    private DBUpdate data() {
        return plugin.getData();
    }

    private void log(SQLException exception) {
        plugin.getLogger().log(Level.SEVERE, "API: database error", exception);
    }
}
