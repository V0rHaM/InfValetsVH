package me.v0rham.infvalets;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.v0rham.infvalets.api.Api;
import me.v0rham.infvalets.convert.ConversionService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Плейсхолдеры InfValets для PlaceholderAPI. Основной поток базу не трогает:
 * значения берутся из кеша, а обновляются асинхронно.
 *
 * <pre>
 *   %infvalets_balance_&lt;валюта&gt;%              баланс игрока
 *   %infvalets_balance_formatted_&lt;валюта&gt;%    баланс с разделителями (1,234,567)
 *   %infvalets_limit_&lt;валюта&gt;%                лимит валюты на игрока
 *   %infvalets_total_&lt;валюта&gt;%                сколько валюты у всех игроков
 *   %infvalets_total_formatted_&lt;валюта&gt;%
 *   %infvalets_rate_&lt;из&gt;_&lt;в&gt;%               текущий курс обмена
 *   %infvalets_top_&lt;валюта&gt;_&lt;1-10&gt;_name%      ник на месте в топе
 *   %infvalets_top_&lt;валюта&gt;_&lt;1-10&gt;_balance%   баланс на месте в топе
 *   %infvalets_top_&lt;валюта&gt;_&lt;1-10&gt;_balance_formatted%
 * </pre>
 */
public class PlaceholdersAPI extends PlaceholderExpansion {

    private static final long PLAYER_TTL_MS = 3000;
    private static final long GLOBAL_TTL_MS = 10000;
    // Защита от бесконечного роста кеша
    private static final int MAX_CACHE_SIZE = 10000;
    // Сколько мест топа доступно через плейсхолдеры
    private static final int TOP_SIZE = 10;

    private interface Loader {
        String load() throws SQLException;
    }

    private static final class Entry {
        final String value;
        final long time;

        Entry(String value, long time) {
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

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String key = params.toLowerCase(Locale.ROOT);

        if (key.startsWith("balance_")) {
            return player == null ? "" : balance(player, key.substring("balance_".length()));
        }
        if (key.startsWith("limit_")) {
            String valet = plugin.findValet(key.substring("limit_".length()));
            return valet == null ? "" : String.valueOf(plugin.getLimit(valet));
        }
        if (key.startsWith("total_")) {
            return total(key.substring("total_".length()));
        }
        if (key.startsWith("rate_")) {
            return rate(key.substring("rate_".length()));
        }
        if (key.startsWith("top_")) {
            return top(key.substring("top_".length()));
        }
        return null;
    }

    // ------------------------------------------------------------ баланс / всего

    private String balance(OfflinePlayer player, String rest) {
        boolean formatted = false;
        String valet = plugin.findValet(rest);
        if (valet == null && rest.startsWith("formatted_")) {
            formatted = true;
            valet = plugin.findValet(rest.substring("formatted_".length()));
        }
        if (valet == null) return "";

        final String name = valet;
        final UUID uuid = player.getUniqueId();
        String raw = cached("balance:" + uuid + ":" + name, "0", PLAYER_TTL_MS,
                () -> String.valueOf(plugin.getData().getBalance(uuid, name)));
        return formatted ? formatNumber(raw) : raw;
    }

    private String total(String rest) {
        boolean formatted = false;
        String valet = plugin.findValet(rest);
        if (valet == null && rest.startsWith("formatted_")) {
            formatted = true;
            valet = plugin.findValet(rest.substring("formatted_".length()));
        }
        if (valet == null) return "";

        final String name = valet;
        String raw = cached("total:" + name, "0", GLOBAL_TTL_MS,
                () -> String.valueOf(plugin.getData().getTotal(name)));
        return formatted ? formatNumber(raw) : raw;
    }

    // ------------------------------------------------------------ курс

    private String rate(String rest) {
        String[] valets = splitPair(rest);
        if (valets == null) return "";

        final ConversionService service = plugin.getConversion();
        final ConversionService.Pair pair = service.getPair(valets[0], valets[1]);
        if (pair == null) return "";

        return cached("rate:" + pair.from + ":" + pair.to, "", GLOBAL_TTL_MS,
                () -> Api.formatRate(service.getRate(pair)));
    }

    // Имена валют могут содержать "_", поэтому пробуем каждое место разделения
    private String[] splitPair(String rest) {
        for (int i = rest.indexOf('_'); i >= 0; i = rest.indexOf('_', i + 1)) {
            String from = plugin.findValet(rest.substring(0, i));
            String to = plugin.findValet(rest.substring(i + 1));
            if (from != null && to != null) return new String[]{from, to};
        }
        return null;
    }

    // ------------------------------------------------------------ топ

    private String top(String rest) {
        String type;
        if (rest.endsWith("_balance_formatted")) {
            type = "balance_formatted";
            rest = rest.substring(0, rest.length() - "_balance_formatted".length());
        } else if (rest.endsWith("_balance")) {
            type = "balance";
            rest = rest.substring(0, rest.length() - "_balance".length());
        } else if (rest.endsWith("_name")) {
            type = "name";
            rest = rest.substring(0, rest.length() - "_name".length());
        } else {
            return "";
        }

        int split = rest.lastIndexOf('_');
        if (split < 0) return "";
        String valet = plugin.findValet(rest.substring(0, split));
        int position;
        try {
            position = Integer.parseInt(rest.substring(split + 1));
        } catch (NumberFormatException exception) {
            return "";
        }
        if (valet == null || position < 1 || position > TOP_SIZE) return "";

        final String name = valet;
        String data = cached("top:" + name, "", GLOBAL_TTL_MS, () -> {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, Long> entry : plugin.getData().top(name, TOP_SIZE)) {
                if (builder.length() > 0) builder.append('\n');
                builder.append(entry.getKey()).append('\t').append(entry.getValue());
            }
            return builder.toString();
        });

        String[] lines = data.isEmpty() ? new String[0] : data.split("\n");
        if (position > lines.length) return "";
        String[] parts = lines[position - 1].split("\t", 2);
        if (parts.length < 2) return "";

        switch (type) {
            case "name":
                return parts[0];
            case "balance":
                return parts[1];
            default:
                return formatNumber(parts[1]);
        }
    }

    // ------------------------------------------------------------ кеш

    private static String formatNumber(String raw) {
        try {
            return Api.format(Long.parseLong(raw));
        } catch (NumberFormatException exception) {
            return raw;
        }
    }

    private String cached(String key, String placeholder, long ttlMillis, Loader loader) {
        Entry entry = cache.get(key);
        if (entry == null || System.currentTimeMillis() - entry.time > ttlMillis) {
            refresh(key, loader);
        }
        // Пока значение не загружено, показываем заглушку
        return entry == null ? placeholder : entry.value;
    }

    private void refresh(String cacheKey, Loader loader) {
        if (!plugin.isEnabled() || !loading.add(cacheKey)) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String value = loader.load();
                if (cache.size() > MAX_CACHE_SIZE) cache.clear();
                cache.put(cacheKey, new Entry(value, System.currentTimeMillis()));
            } catch (SQLException exception) {
                plugin.getLogger().warning("Placeholder lookup failed: " + exception.getMessage());
            } finally {
                loading.remove(cacheKey);
            }
        });
    }
}
