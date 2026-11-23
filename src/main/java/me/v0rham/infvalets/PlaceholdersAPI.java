package me.v0rham.infvalets;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlaceholdersAPI extends PlaceholderExpansion {

    // Как долго значение из кеша считается свежим
    private static final long TTL_MS = 3000;
    // Защита от бесконечного роста кеша
    private static final int MAX_CACHE_SIZE = 10000;

    private static final class Entry {
        final long value;
        final long time;

        Entry(long value, long time) {
            this.value = value;
            this.time = time;
        }
    }

    private final Main plugin;
    private final Map<String, Entry> cache = new ConcurrentHashMap<>();
    private final Set<String> loading = ConcurrentHashMap.newKeySet();

    public PlaceholdersAPI(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getIdentifier() {
        return "infvalets";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    // %infvalets_balance_<valet_name>%
    // Основной поток базу не трогает: отдаём значение из кеша, а обновление идёт асинхронно.
    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";

        String key = params.toLowerCase(Locale.ROOT);
        if (key.startsWith("balance_")) {
            String valet = plugin.findValet(key.substring("balance_".length()));
            if (valet == null) return "";

            String cacheKey = player.getUniqueId() + ":" + valet;
            Entry entry = cache.get(cacheKey);
            if (entry == null || System.currentTimeMillis() - entry.time > TTL_MS) {
                refresh(player.getUniqueId(), valet, cacheKey);
            }
            // Пока значение ещё не загружено, показываем 0
            return entry == null ? "0" : String.valueOf(entry.value);
        }
        return null;
    }

    private void refresh(UUID uuid, String valet, String cacheKey) {
        if (!plugin.isEnabled() || !loading.add(cacheKey)) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                long value = plugin.getData().getBalance(uuid, valet);
                if (cache.size() > MAX_CACHE_SIZE) cache.clear();
                cache.put(cacheKey, new Entry(value, System.currentTimeMillis()));
            } catch (SQLException exception) {
                plugin.getLogger().warning("Placeholder balance lookup failed: " + exception.getMessage());
            } finally {
                loading.remove(cacheKey);
            }
        });
    }
}
