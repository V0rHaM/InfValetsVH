package me.v0rham.infvalets.commands.balance;

import me.v0rham.infvalets.Main;
import me.v0rham.infvalets.api.Api;
import me.v0rham.infvalets.api.InfValetsAPI;
import me.v0rham.infvalets.api.TransactionResult;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

/** Админские операции: give / take / set (идут через API, поэтому вызывают события). */
public class Money {

    public static void give(CommandSender sender, OfflinePlayer target, String valet, long amount) {
        TransactionResult result = api().deposit(target.getUniqueId(), target.getName(), valet, amount);
        report(sender, result, "Messages.admin-give", target, valet, amount);
    }

    public static void take(CommandSender sender, OfflinePlayer target, String valet, long amount) {
        TransactionResult result = api().withdraw(target.getUniqueId(), valet, amount);
        report(sender, result, "Messages.admin-take", target, valet, amount);
    }

    public static void set(CommandSender sender, OfflinePlayer target, String valet, long amount) {
        TransactionResult result = api().set(target.getUniqueId(), target.getName(), valet, amount);
        report(sender, result, "Messages.admin-set", target, valet, amount);
    }

    private static InfValetsAPI api() {
        return Main.getInstance().getApi();
    }

    private static void report(CommandSender sender, TransactionResult result, String path,
                               OfflinePlayer target, String valet, long amount) {
        if (!result.isSuccess()) {
            Api.sendError(sender, result);
            return;
        }
        sender.sendMessage(Api.getMessage(path, true,
                "%player%", Api.nameOf(target), "%valet%", valet, "%amount%", Api.format(amount)));
    }
}
