package me.v0rham.infvalets.commands.balance;

import me.v0rham.infvalets.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** /baltop [валюта] — то же, что /balance top [валюта]. */
public class BalTop implements CommandExecutor, TabCompleter {

    private final Balance balance = new Balance();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String[] shifted = new String[args.length + 1];
        shifted[0] = "top";
        System.arraycopy(args, 0, shifted, 1, args.length);
        return balance.onCommand(sender, command, label, shifted);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Main.getInstance().getValets(), result);
            Collections.sort(result);
        }
        return result;
    }
}
