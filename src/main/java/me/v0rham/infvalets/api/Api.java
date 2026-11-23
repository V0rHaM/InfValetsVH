package me.v0rham.infvalets.api;

import me.v0rham.infvalets.Main;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

public final class Api {

    private Api() {
    }

    /**
     * Берёт текст из config.yml, подставляет пары "%ключ%", "значение" и красит цвета.
     * Пример: getMessage("Messages.pay-sent", true, "%player%", "Steve", "%amount%", "10")
     */
    public static String getMessage(String path, boolean prefix, String... replacements) {
        Main plugin = Main.getInstance();
        FileConfiguration config = plugin.getConfig();

        String msg = config.getString(path);
        if (msg == null) {
            plugin.getLogger().warning("Text not found in config.yml: [" + path + "]");
            return translateColor("&cОшибка: &7Текст не найден! Пожалуйста, обратитесь к администрации!");
        }

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        if (prefix) msg = config.getString("Messages.prefix", "") + " " + msg;

        return translateColor(msg);
    }

    // Свой перевод &-кодов в §-коды: не зависит от устаревающего org.bukkit.ChatColor
    public static String translateColor(String text) {
        if (text == null) return "";
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(chars[i + 1]) > -1) {
                chars[i] = '\u00A7';
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    /** Возвращает сумму или -1, если строка не число / не положительная (0 разрешён при allowZero). */
    public static long parseAmount(String text, boolean allowZero) {
        try {
            long value = Long.parseLong(text);
            if (value < 0 || (value == 0 && !allowZero)) return -1;
            return value;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    /** Курс без лишних нулей: 10, 0.0952 */
    public static String formatRate(double rate) {
        return new BigDecimal(rate).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    public static String format(long amount) {
        return String.format(Locale.ROOT, "%,d", amount);
    }

    /**
     * Ищет игрока по нику: онлайн -> база InfValets -> известные серверу офлайн-игроки.
     * Работает на Spigot и Paper любых версий. null — не найден.
     */
    public static OfflinePlayer resolvePlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;

        Main plugin = Main.getInstance();
        try {
            UUID uuid = plugin.getData().findUuid(name, plugin.getValets());
            if (uuid != null) return Bukkit.getOfflinePlayer(uuid);
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Player lookup failed", exception);
        }

        for (OfflinePlayer known : Bukkit.getOfflinePlayers()) {
            if (known.getName() != null && known.getName().equalsIgnoreCase(name)) return known;
        }
        return null;
    }

    public static String nameOf(OfflinePlayer player) {
        return player.getName() != null ? player.getName() : player.getUniqueId().toString();
    }

    /** Отправляет отправителю сообщение об ошибке, соответствующее результату операции. */
    public static void sendError(CommandSender sender, TransactionResult result) {
        String path;
        switch (result) {
            case INSUFFICIENT_FUNDS:
                path = "Messages.Errors.not-enough";
                break;
            case LIMIT_REACHED:
                path = "Messages.Errors.limit-reached";
                break;
            case UNKNOWN_VALET:
                path = "Messages.Errors.unknown-valet";
                break;
            case INVALID_AMOUNT:
                path = "Messages.Errors.invalid-amount";
                break;
            case SAME_ACCOUNT:
                path = "Messages.Errors.pay-self";
                break;
            case CANCELLED:
                path = "Messages.Errors.cancelled";
                break;
            case NOT_CONVERTIBLE:
                path = "Messages.Errors.convert-unavailable";
                break;
            case TOO_SMALL:
                path = "Messages.Errors.convert-too-small";
                break;
            default:
                path = "Messages.Errors.database";
                break;
        }
        sender.sendMessage(getMessage(path, true));
    }

    public static void dbError(CommandSender sender, SQLException exception) {
        Main.getInstance().getLogger().log(Level.SEVERE, "Database error", exception);
        sender.sendMessage(getMessage("Messages.Errors.database", true));
    }
}
