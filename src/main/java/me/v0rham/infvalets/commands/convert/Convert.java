package me.v0rham.infvalets.commands.convert;

import me.v0rham.infvalets.Main;
import me.v0rham.infvalets.api.Api;
import me.v0rham.infvalets.api.ConversionResult;
import me.v0rham.infvalets.convert.ConversionService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * /convert <из> <в> <сумма>   — обменять валюту
 * /convert rate [из] [в]      — текущие курсы
 */
public class Convert implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("infvalets.convert")) {
            sender.sendMessage(Api.getMessage("Messages.Errors.not-permission", true));
            return true;
        }
        Main plugin = Main.getInstance();

        if (args.length >= 1 && args[0].equalsIgnoreCase("rate")) {
            showRates(sender, args);
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(Api.getMessage("Messages.Errors.only-players", true));
            return true;
        }
        if (args.length != 3) {
            sender.sendMessage(Api.getMessage("Messages.usage-convert", true));
            return true;
        }

        String from = plugin.findValet(args[0]);
        String to = plugin.findValet(args[1]);
        if (from == null || to == null) {
            sender.sendMessage(Api.getMessage("Messages.Errors.unknown-valet", true));
            return true;
        }
        long amount = Api.parseAmount(args[2], false);
        if (amount < 0) {
            sender.sendMessage(Api.getMessage("Messages.Errors.invalid-amount", true));
            return true;
        }

        Player player = (Player) sender;
        ConversionResult result = plugin.getApi().convert(player.getUniqueId(), player.getName(), from, to, amount);
        if (!result.isSuccess()) {
            Api.sendError(sender, result.getStatus());
            return true;
        }
        sender.sendMessage(Api.getMessage("Messages.convert-done", true,
                "%from_amount%", Api.format(result.getSpent()), "%from%", from,
                "%to_amount%", Api.format(result.getReceived()), "%to%", to,
                "%rate%", Api.formatRate(result.getRate())));
        return true;
    }

    private void showRates(CommandSender sender, String[] args) {
        Main plugin = Main.getInstance();
        try {
            if (args.length == 1) { // список всех пар
                sender.sendMessage(Api.getMessage("Messages.convert-rate-header", false));
                ConversionService service = plugin.getConversion();
                for (ConversionService.Pair pair : service.getPairs()) {
                    sender.sendMessage(Api.getMessage("Messages.convert-rate", false,
                            "%from%", pair.from, "%to%", pair.to,
                            "%rate%", Api.formatRate(service.getRate(pair))));
                }
                return;
            }
            if (args.length != 3) {
                sender.sendMessage(Api.getMessage("Messages.usage-convert", true));
                return;
            }
            String from = plugin.findValet(args[1]);
            String to = plugin.findValet(args[2]);
            if (from == null || to == null) {
                sender.sendMessage(Api.getMessage("Messages.Errors.unknown-valet", true));
                return;
            }
            double rate = plugin.getApi().getRate(from, to);
            if (rate < 0) {
                sender.sendMessage(Api.getMessage("Messages.Errors.convert-unavailable", true));
                return;
            }
            sender.sendMessage(Api.getMessage("Messages.convert-rate", true,
                    "%from%", from, "%to%", to, "%rate%", Api.formatRate(rate)));
        } catch (SQLException exception) {
            Api.dbError(sender, exception);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        Main plugin = Main.getInstance();
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("rate");
            options.addAll(plugin.getValets());
        } else if (args.length == 2 || args.length == 3 && args[0].equalsIgnoreCase("rate")) {
            options.addAll(plugin.getValets());
        } else if (args.length == 3 && sender instanceof Player) {
            options.add("100");
        }
        List<String> result = new ArrayList<>();
        StringUtil.copyPartialMatches(args[args.length - 1], options, result);
        Collections.sort(result);
        return result;
    }
}
