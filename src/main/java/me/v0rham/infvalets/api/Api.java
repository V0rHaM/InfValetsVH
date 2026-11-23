package me.v0rham.infvalets.api;

import me.v0rham.infvalets.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Locale;
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

    public static String translateColor(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
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

    public static String format(long amount) {
        return String.format(Locale.ROOT, "%,d", amount);
    }

    /** Онлайн-игрок или офлайн-игрок, который уже заходил на сервер. null — не найден. */
    public static OfflinePlayer resolvePlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    public static String nameOf(OfflinePlayer player) {
        return player.getName() != null ? player.getName() : player.getUniqueId().toString();
    }

    public static void dbError(CommandSender sender, SQLException exception) {
        Main.getInstance().getLogger().log(Level.SEVERE, "Database error", exception);
        sender.sendMessage(getMessage("Messages.Errors.database", true));
    }
}
