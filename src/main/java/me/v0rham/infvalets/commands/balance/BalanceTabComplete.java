package me.v0rham.infvalets.commands.balance;

import me.v0rham.infvalets.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class BalanceTabComplete implements TabCompleter {

    private static final List<String> ADMIN_ACTIONS = Arrays.asList("give", "take", "set");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        Main plugin = Main.getInstance();
        boolean admin = sender.hasPermission("infvalets.admin");
        boolean others = sender.hasPermission("infvalets.balance.others");
        List<String> options = new ArrayList<>();

        if (args.length == 1) {
            options.add("top");
            options.addAll(plugin.getValets());
            if (admin) options.addAll(ADMIN_ACTIONS);
            if (others) options.addAll(onlinePlayers());
        } else if (args.length == 2) {
            String first = args[0].toLowerCase(Locale.ROOT);
            if (first.equals("top")) {
                options.addAll(plugin.getValets());
            } else if (ADMIN_ACTIONS.contains(first)) {
                if (admin) options.addAll(onlinePlayers());
            } else if (others) {
                options.addAll(plugin.getValets());
            }
        } else if (args.length == 3 && admin && ADMIN_ACTIONS.contains(args[0].toLowerCase(Locale.ROOT))) {
            options.addAll(plugin.getValets());
        }

        List<String> result = new ArrayList<>();
        StringUtil.copyPartialMatches(args[args.length - 1], options, result);
        Collections.sort(result);
        return result;
    }

    private List<String> onlinePlayers() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName());
        return names;
    }
}
