package me.v0rham.infvalets.commands.balance;

import me.v0rham.infvalets.Main;
import me.v0rham.infvalets.api.Api;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;

/** Админские операции: give / take / set. */
public class Money {

    public static void give(CommandSender sender, OfflinePlayer target, String valet, long amount) throws SQLException {
        Main plugin = Main.getInstance();
        boolean ok = plugin.getData().add(target.getUniqueId(), target.getName(), valet, amount, plugin.getLimit(valet));
        if (ok) {
            sender.sendMessage(result("Messages.admin-give", target, valet, amount));
        } else {
            sender.sendMessage(Api.getMessage("Messages.Errors.limit-reached", true));
        }
    }

    public static void take(CommandSender sender, OfflinePlayer target, String valet, long amount) throws SQLException {
        boolean ok = Main.getInstance().getData().take(target.getUniqueId(), valet, amount);
        if (ok) {
            sender.sendMessage(result("Messages.admin-take", target, valet, amount));
        } else {
            sender.sendMessage(Api.getMessage("Messages.Errors.not-enough", true));
        }
    }

    public static void set(CommandSender sender, OfflinePlayer target, String valet, long amount) throws SQLException {
        Main plugin = Main.getInstance();
        if (amount > plugin.getLimit(valet)) {
            sender.sendMessage(Api.getMessage("Messages.Errors.limit-reached", true));
            return;
        }
        plugin.getData().set(target.getUniqueId(), target.getName(), valet, amount);
        sender.sendMessage(result("Messages.admin-set", target, valet, amount));
    }

    private static String result(String path, OfflinePlayer target, String valet, long amount) {
        return Api.getMessage(path, true,
                "%player%", Api.nameOf(target), "%valet%", valet, "%amount%", Api.format(amount));
    }
}
