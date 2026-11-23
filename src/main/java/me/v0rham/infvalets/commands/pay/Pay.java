package me.v0rham.infvalets.commands.pay;

import me.v0rham.infvalets.Main;
import me.v0rham.infvalets.api.Api;
import me.v0rham.infvalets.api.TransactionResult;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** /pay <игрок> [валюта] <сумма>  (валюту можно не указывать, если она в плагине одна) */
public class Pay implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Api.getMessage("Messages.Errors.only-players", true));
            return true;
        }
        if (!sender.hasPermission("infvalets.pay")) {
            sender.sendMessage(Api.getMessage("Messages.Errors.not-permission", true));
            return true;
        }
        if (args.length < 2 || args.length > 3) {
            sender.sendMessage(Api.getMessage("Messages.usage-pay", true));
            return true;
        }

        Main plugin = Main.getInstance();
        Player from = (Player) sender;

        OfflinePlayer target = Api.resolvePlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Api.getMessage("Messages.Errors.player-not-found", true));
            return true;
        }

        String valet;
        String amountText;
        if (args.length == 3) {
            valet = plugin.findValet(args[1]);
            amountText = args[2];
            if (valet == null) {
                sender.sendMessage(Api.getMessage("Messages.Errors.unknown-valet", true));
                return true;
            }
        } else {
            valet = plugin.getDefaultValet();
            amountText = args[1];
            if (valet == null) { // валют несколько — нужно указать явно
                sender.sendMessage(Api.getMessage("Messages.usage-pay", true));
                return true;
            }
        }

        long amount = Api.parseAmount(amountText, false);
        if (amount < 0) {
            sender.sendMessage(Api.getMessage("Messages.Errors.invalid-amount", true));
            return true;
        }

        TransactionResult result = plugin.getApi().transfer(
                from.getUniqueId(), target.getUniqueId(), target.getName(), valet, amount);
        if (!result.isSuccess()) {
            Api.sendError(sender, result);
            return true;
        }

        sender.sendMessage(Api.getMessage("Messages.pay-sent", true,
                "%player%", Api.nameOf(target), "%valet%", valet, "%amount%", Api.format(amount)));
        Player online = target.getPlayer();
        if (online != null) {
            online.sendMessage(Api.getMessage("Messages.pay-received", true,
                    "%player%", from.getName(), "%valet%", valet, "%amount%", Api.format(amount)));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) options.add(player.getName());
        } else if (args.length == 2) {
            options.addAll(Main.getInstance().getValets());
        }
        List<String> result = new ArrayList<>();
        StringUtil.copyPartialMatches(args[args.length - 1], options, result);
        Collections.sort(result);
        return result;
    }
}
