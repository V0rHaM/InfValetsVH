package me.v0rham.infvalets.commands.eco;

import me.v0rham.infvalets.Main;
import me.v0rham.infvalets.api.Api;
import me.v0rham.infvalets.commands.balance.Money;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * /eco give|take|set <игрок> [валюта] <сумма>
 * /eco reset <игрок> [валюта]
 * Валюту можно не указывать, если она в плагине одна. Право: infvalets.admin.
 */
public class Eco implements CommandExecutor, TabCompleter {

    private static final List<String> ACTIONS = Arrays.asList("give", "take", "set", "reset");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("infvalets.admin")) {
            sender.sendMessage(Api.getMessage("Messages.Errors.not-permission", true));
            return true;
        }
        if (args.length < 2 || !ACTIONS.contains(args[0].toLowerCase(Locale.ROOT))) {
            sender.sendMessage(Api.getMessage("Messages.usage-eco", true));
            return true;
        }

        Main plugin = Main.getInstance();
        String action = args[0].toLowerCase(Locale.ROOT);
        boolean reset = action.equals("reset");

        // Остаток аргументов после игрока: [валюта] <сумма>  (для reset только [валюта])
        String[] rest = Arrays.copyOfRange(args, 2, args.length);
        String valetArg = null;
        String amountArg = null;
        if (reset) {
            if (rest.length > 1) {
                sender.sendMessage(Api.getMessage("Messages.usage-eco", true));
                return true;
            }
            if (rest.length == 1) valetArg = rest[0];
        } else if (rest.length == 1) {
            amountArg = rest[0];
        } else if (rest.length == 2) {
            valetArg = rest[0];
            amountArg = rest[1];
        } else {
            sender.sendMessage(Api.getMessage("Messages.usage-eco", true));
            return true;
        }

        OfflinePlayer target = Api.resolvePlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Api.getMessage("Messages.Errors.player-not-found", true));
            return true;
        }

        String valet;
        if (valetArg != null) {
            valet = plugin.findValet(valetArg);
            if (valet == null) {
                sender.sendMessage(Api.getMessage("Messages.Errors.unknown-valet", true));
                return true;
            }
        } else {
            valet = plugin.getDefaultValet();
            if (valet == null) { // валют несколько — нужно указать явно
                sender.sendMessage(Api.getMessage("Messages.usage-eco", true));
                return true;
            }
        }

        long amount = 0;
        if (!reset) {
            amount = Api.parseAmount(amountArg, action.equals("set"));
            if (amount < 0) {
                sender.sendMessage(Api.getMessage("Messages.Errors.invalid-amount", true));
                return true;
            }
        }

        switch (action) {
            case "give":
                Money.give(sender, target, valet, amount);
                break;
            case "take":
                Money.take(sender, target, valet, amount);
                break;
            default: // set и reset (reset = set 0)
                Money.set(sender, target, valet, amount);
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (sender.hasPermission("infvalets.admin")) {
            if (args.length == 1) {
                options.addAll(ACTIONS);
            } else if (args.length == 2) {
                for (Player player : Bukkit.getOnlinePlayers()) options.add(player.getName());
            } else if (args.length == 3) {
                options.addAll(Main.getInstance().getValets());
            }
        }
        List<String> result = new ArrayList<>();
        StringUtil.copyPartialMatches(args[args.length - 1], options, result);
        Collections.sort(result);
        return result;
    }
}
