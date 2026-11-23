package me.v0rham.infvalets.api;

import me.v0rham.infvalets.Main;
import me.v0rham.infvalets.database.DBUpdate;
import org.bukkit.Bukkit;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Реализация {@link InfValetsAPI}. Регистрируется в ServicesManager в Main#onEnable.
 * Через неё же работают и команды плагина, поэтому события вызываются для любых изменений баланса.
 */
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
        if (!callPre(TransactionType.DEPOSIT, uuid, null, resolved, amount)) return TransactionResult.CANCELLED;
        try {
            if (!data().add(uuid, playerName, resolved, amount, plugin.getLimit(resolved))) {
                return TransactionResult.LIMIT_REACHED;
            }
        } catch (SQLException exception) {
            log(exception);
            return TransactionResult.ERROR;
        }
        callPost(TransactionType.DEPOSIT, uuid, null, resolved, amount);
        return TransactionResult.SUCCESS;
    }

    @Override
    public TransactionResult withdraw(UUID uuid, String valet, long amount) {
        String resolved = plugin.findValet(valet);
        if (resolved == null) return TransactionResult.UNKNOWN_VALET;
        if (amount <= 0) return TransactionResult.INVALID_AMOUNT;
        if (!plugin.isEnabled()) return TransactionResult.ERROR;
        if (!callPre(TransactionType.WITHDRAW, uuid, null, resolved, amount)) return TransactionResult.CANCELLED;
        try {
            if (!data().take(uuid, resolved, amount)) return TransactionResult.INSUFFICIENT_FUNDS;
        } catch (SQLException exception) {
            log(exception);
            return TransactionResult.ERROR;
        }
        callPost(TransactionType.WITHDRAW, uuid, null, resolved, amount);
        return TransactionResult.SUCCESS;
    }

    @Override
    public TransactionResult set(UUID uuid, String playerName, String valet, long amount) {
        String resolved = plugin.findValet(valet);
        if (resolved == null) return TransactionResult.UNKNOWN_VALET;
        if (amount < 0) return TransactionResult.INVALID_AMOUNT;
        if (amount > plugin.getLimit(resolved)) return TransactionResult.LIMIT_REACHED;
        if (!plugin.isEnabled()) return TransactionResult.ERROR;
        if (!callPre(TransactionType.SET, uuid, null, resolved, amount)) return TransactionResult.CANCELLED;
        try {
            data().set(uuid, playerName, resolved, amount);
        } catch (SQLException exception) {
            log(exception);
            return TransactionResult.ERROR;
        }
        callPost(TransactionType.SET, uuid, null, resolved, amount);
        return TransactionResult.SUCCESS;
    }

    @Override
    public TransactionResult transfer(UUID from, UUID to, String toName, String valet, long amount) {
        String resolved = plugin.findValet(valet);
        if (resolved == null) return TransactionResult.UNKNOWN_VALET;
        if (amount <= 0) return TransactionResult.INVALID_AMOUNT;
        if (from.equals(to)) return TransactionResult.SAME_ACCOUNT;
        if (!plugin.isEnabled()) return TransactionResult.ERROR;
        if (!callPre(TransactionType.TRANSFER, to, from, resolved, amount)) return TransactionResult.CANCELLED;
        try {
            DBUpdate.TransferResult result =
                    data().transfer(from, to, toName, resolved, amount, plugin.getLimit(resolved));
            switch (result) {
                case NOT_ENOUGH:
                    return TransactionResult.INSUFFICIENT_FUNDS;
                case LIMIT:
                    return TransactionResult.LIMIT_REACHED;
                default:
                    break;
            }
        } catch (SQLException exception) {
            log(exception);
            return TransactionResult.ERROR;
        }
        callPost(TransactionType.TRANSFER, to, from, resolved, amount);
        return TransactionResult.SUCCESS;
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

    // true — можно продолжать, false — событие отменили
    private boolean callPre(TransactionType type, UUID target, UUID source, String valet, long amount) {
        ValetTransactionEvent event = new ValetTransactionEvent(
                !Bukkit.isPrimaryThread(), type, target, source, valet, amount);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    private void callPost(TransactionType type, UUID target, UUID source, String valet, long amount) {
        Bukkit.getPluginManager().callEvent(new ValetBalanceChangedEvent(
                !Bukkit.isPrimaryThread(), type, target, source, valet, amount));
    }

    private DBUpdate data() {
        return plugin.getData();
    }

    private void log(SQLException exception) {
        plugin.getLogger().log(Level.SEVERE, "API: database error", exception);
    }
}
