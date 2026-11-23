package me.v0rham.infvalets.convert;

import me.v0rham.infvalets.Main;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Курсы обмена валют: фиксированные (FIXED) и динамические (DYNAMIC).
 * <p>
 * Курс — сколько единиц валюты «в» даётся за 1 единицу валюты «из».
 * Динамический курс:
 * <pre>
 *   supplyFactor = 1 + supplySens * (общее_кол-во_"в" / supplyRef - 1)      (больше валюты -> она дешевле -> курс выше)
 *   demandFactor = 1 / (1 + demandSens * (операций_за_окно / demandRef - 1)) (частые обмены "в" -> она дороже -> курс ниже)
 *   rate = clamp(base * supplyFactor * demandFactor, min-rate, max-rate)
 * </pre>
 */
public class ConversionService {

    public static final class Pair {
        public final String from;
        public final String to;
        public final boolean dynamic;
        public final double rate;
        public final double minRate;
        public final double maxRate;
        public final double supplyReference;
        public final double supplySensitivity;
        public final double demandReference;
        public final double demandSensitivity;
        public final long windowMillis;
        public final double fee;

        Pair(String from, String to, boolean dynamic, double rate, double minRate, double maxRate,
             double supplyReference, double supplySensitivity,
             double demandReference, double demandSensitivity, long windowMillis, double fee) {
            this.from = from;
            this.to = to;
            this.dynamic = dynamic;
            this.rate = rate;
            this.minRate = minRate;
            this.maxRate = maxRate;
            this.supplyReference = supplyReference;
            this.supplySensitivity = supplySensitivity;
            this.demandReference = demandReference;
            this.demandSensitivity = demandSensitivity;
            this.windowMillis = windowMillis;
            this.fee = fee;
        }
    }

    // Общее количество валюты считается по всей таблице, поэтому кешируем
    private static final long SUPPLY_TTL_MS = 30_000;

    private final Main plugin;
    private final Map<String, long[]> supplyCache = new ConcurrentHashMap<>();

    public ConversionService(Main plugin) {
        this.plugin = plugin;
    }

    /** Настройки пары «из» -> «в» или null, если такой конвертации нет/она выключена/настроена неверно. */
    public Pair getPair(String from, String to) {
        if (from.equalsIgnoreCase(to) || !plugin.canConvert(from) || !plugin.canConvert(to)) return null;
        ConfigurationSection pairs = plugin.getConfig().getConfigurationSection("Convert.pairs");
        if (pairs == null) return null;
        for (String key : pairs.getKeys(false)) {
            if (key.equalsIgnoreCase(from + "-" + to)) {
                return parse(key, from, to, pairs.getConfigurationSection(key));
            }
        }
        return null;
    }

    /** Все рабочие пары из конфига. */
    public List<Pair> getPairs() {
        List<Pair> result = new ArrayList<>();
        ConfigurationSection pairs = plugin.getConfig().getConfigurationSection("Convert.pairs");
        if (pairs == null) return result;
        for (String key : pairs.getKeys(false)) {
            String[] parts = key.split("-", 2);
            if (parts.length != 2) continue;
            String from = plugin.findValet(parts[0]);
            String to = plugin.findValet(parts[1]);
            if (from == null || to == null) continue;
            Pair pair = getPair(from, to);
            if (pair != null) result.add(pair);
        }
        return result;
    }

    private Pair parse(String key, String from, String to, ConfigurationSection section) {
        if (section == null) return null;

        String mode = section.getString("mode", "FIXED").toUpperCase(Locale.ROOT);
        boolean dynamic = mode.equals("DYNAMIC");
        if (!dynamic && !mode.equals("FIXED")) {
            plugin.getLogger().warning("Convert.pairs." + key + ": неизвестный mode '" + mode + "' (FIXED или DYNAMIC)");
            return null;
        }

        double rate = section.getDouble("rate", 0);
        if (!(rate > 0)) {
            plugin.getLogger().warning("Convert.pairs." + key + ": rate должен быть больше 0");
            return null;
        }
        double fee = Math.min(100, Math.max(0,
                section.getDouble("fee-percent", plugin.getConfig().getDouble("Convert.fee-percent", 0))));

        if (!dynamic) {
            return new Pair(from, to, false, rate, rate, rate, 0, 0, 0, 0, 0, fee);
        }

        double min = section.getDouble("min-rate", rate * 0.1);
        double max = section.getDouble("max-rate", rate * 10);
        if (!(min > 0) || max < min) {
            plugin.getLogger().warning("Convert.pairs." + key + ": нужно 0 < min-rate <= max-rate");
            return null;
        }
        long minutes = Math.min(43200, Math.max(1, section.getLong("demand.window-minutes", 60)));
        return new Pair(from, to, true, rate, min, max,
                section.getDouble("supply.reference", 0), section.getDouble("supply.sensitivity", 0),
                section.getDouble("demand.reference-ops", 0), section.getDouble("demand.sensitivity", 0),
                minutes * 60_000L, fee);
    }

    /** Текущий курс пары (для FIXED — константа, для DYNAMIC — по формуле). */
    public double getRate(Pair pair) throws SQLException {
        if (!pair.dynamic) return pair.rate;

        double factor = 1.0;
        if (pair.supplyReference > 0 && pair.supplySensitivity > 0) {
            double supply = getSupply(pair.to);
            factor *= Math.max(0.01, 1 + pair.supplySensitivity * (supply / pair.supplyReference - 1));
        }
        if (pair.demandReference > 0 && pair.demandSensitivity > 0) {
            long since = System.currentTimeMillis() - pair.windowMillis;
            long operations = plugin.getData().countConversions(pair.to, since);
            factor /= Math.max(0.1, 1 + pair.demandSensitivity * (operations / pair.demandReference - 1));
        }
        return Math.min(pair.maxRate, Math.max(pair.minRate, pair.rate * factor));
    }

    /** Сколько «в» получит игрок за amount «из» (с комиссией, вниз). -1 — результат слишком велик. */
    public static long calculate(Pair pair, long amount, double rate) {
        double result = Math.floor(amount * rate * (1 - pair.fee / 100.0));
        if (result > 9.0e15) return -1;
        return (long) result;
    }

    private long getSupply(String valet) throws SQLException {
        long now = System.currentTimeMillis();
        long[] cached = supplyCache.get(valet);
        if (cached != null && now - cached[1] <= SUPPLY_TTL_MS) return cached[0];
        long total = plugin.getData().getTotal(valet);
        supplyCache.put(valet, new long[]{total, now});
        return total;
    }

    /** Сбрасывает кеш общего количества валюты (после конвертации оно изменилось). */
    public void invalidate(String valet) {
        supplyCache.remove(valet);
    }

    /**
     * Предупреждает в консоли, если встречные пары позволяют зарабатывать на кругообороте
     * (произведение максимальных курсов с учётом комиссий больше 1).
     */
    public void validate() {
        List<Pair> all = getPairs();
        for (Pair a : all) {
            for (Pair b : all) {
                if (!a.from.equals(b.to) || !a.to.equals(b.from) || a.from.compareTo(a.to) >= 0) continue;
                double product = a.maxRate * (1 - a.fee / 100.0) * b.maxRate * (1 - b.fee / 100.0);
                if (product > 1.0) {
                    plugin.getLogger().warning("Пары " + a.from + "-" + a.to + " и " + b.from + "-" + b.to
                            + " позволяют зарабатывать на обмене туда-обратно (произведение курсов "
                            + String.format(Locale.ROOT, "%.3f", product)
                            + " > 1). Уменьшите max-rate или добавьте fee-percent.");
                }
            }
        }
    }
}
