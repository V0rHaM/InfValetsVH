package me.v0rham.infvalets.commands.balance;

import me.v0rham.infvalets.Main;
import me.v0rham.infvalets.api.Api;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /balance                            - свои балансы
 * /balance <валюта>                   - свой баланс в валюте
 * /balance <игрок> [валюта]           - чужой баланс (infvalets.balance.others)
 * /balance top [валюта]               - топ
 * /balance give|take|set <игрок> <валюта> <сумма>  (infvalets.admin)
 */
public class Balance implements CommandExecutor {

    private static final int TOP_SIZE = 10;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("infvalets.balance")) {
            sender.sendMessage(Api.getMessage("Messages.Errors.not-permission", true));
            return true;
        }

        try {
            if (args.length == 0) {
                if (sender instanceof Player) {
                    showBalances(sender, (Player) sender, true, null);
                } else {
                    sender.sendMessage(Api.getMessage("Messages.usage-balance", true));
                }
                return true;
            }

            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "top":
                    showTop(sender, args);
                    return true;
                case "give":
                case "take":
                case "set":
                    admin(sender, args);
                    return true;
                default:
                    break;
            }

            Main plugin = Main.getInstance();

            // /balance <валюта>
            String valet = plugin.findValet(args[0]);
            if (valet != null) {
                if (sender instanceof Player) {
                    showBalances(sender, (Player) sender, true, valet);
                } else {
                    sender.sendMessage(Api.getMessage("Messages.Errors.only-players", true));
                }
                return true;
            }

            // /balance <игрок> [валюта]
            if (!sender.hasPermission("infvalets.balance.others")) {
                sender.sendMessage(Api.getMessage("Messages.Errors.not-permission", true));
                return true;
            }
            OfflinePlayer target = Api.resolvePlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Api.getMessage("Messages.Errors.player-not-found", true));
                return true;
            }
            String onlyValet = null;
            if (args.length > 1) {
                onlyValet = plugin.findValet(args[1]);
                if (onlyValet == null) {
                    sender.sendMessage(Api.getMessage("Messages.Errors.unknown-valet", true));
                    return true;
                }
            }
            showBalances(sender, target, false, onlyValet);
        } catch (SQLException exception) {
            Api.dbError(sender, exception);
        }
        return true;
    }

    private void showBalances(CommandSender to, OfflinePlayer target, boolean self, String onlyValet) throws SQLException {
        Main plugin = Main.getInstance();
        to.sendMessage(self
                ? Api.getMessage("Messages.balance-header", false)
                : Api.getMessage("Messages.balance-header-other", false, "%player%", Api.nameOf(target)));

        for (String valet : plugin.getValets()) {
            if (onlyValet != null && !valet.equals(onlyValet)) continue;
            long amount = plugin.getData().getBalance(target.getUniqueId(), valet);
            to.sendMessage(Api.getMessage("Messages.balance-line", false,
                    "%valet%", valet, "%amount%", Api.format(amount)));
        }
    }

    private void showTop(CommandSender sender, String[] args) throws SQLException {
        Main plugin = Main.getInstance();
        String valet = args.length > 1 ? plugin.findValet(args[1]) : plugin.getDefaultValet();
        if (valet == null) {
            sender.sendMessage(Api.getMessage(args.length > 1
                    ? "Messages.Errors.unknown-valet" : "Messages.usage-top", true));
            return;
        }

        List<Map.Entry<String, Long>> top = plugin.getData().top(valet, TOP_SIZE);
        sender.sendMessage(Api.getMessage("Messages.top-header", false, "%valet%", valet));
        if (top.isEmpty()) {
            sender.sendMessage(Api.getMessage("Messages.top-empty", false));
            return;
        }
        int pos = 1;
        for (Map.Entry<String, Long> entry : top) {
            sender.sendMessage(Api.getMessage("Messages.top-line", false,
                    "%pos%", String.valueOf(pos++), "%player%", entry.getKey(),
                    "%amount%", Api.format(entry.getValue())));
        }
    }

    private void admin(CommandSender sender, String[] args) throws SQLException {
        if (!sender.hasPermission("infvalets.admin")) {
            sender.sendMessage(Api.getMessage("Messages.Errors.not-permission", true));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(Api.getMessage("Messages.usage-admin", true));
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        OfflinePlayer target = Api.resolvePlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Api.getMessage("Messages.Errors.player-not-found", true));
            return;
        }
        String valet = Main.getInstance().findValet(args[2]);
        if (valet == null) {
            sender.sendMessage(Api.getMessage("Messages.Errors.unknown-valet", true));
            return;
        }
        long amount = Api.parseAmount(args[3], action.equals("set"));
        if (amount < 0) {
            sender.sendMessage(Api.getMessage("Messages.Errors.invalid-amount", true));
            return;
        }

        switch (action) {
            case "give":
                Money.give(sender, target, valet, amount);
                break;
            case "take":
                Money.take(sender, target, valet, amount);
                break;
            default:
                Money.set(sender, target, valet, amount);
                break;
        }
    }
}
