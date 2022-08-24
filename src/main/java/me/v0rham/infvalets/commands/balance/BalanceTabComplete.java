package me.v0rham.infvalets.commands.balance;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BalanceTabComplete implements TabCompleter {

    private static final String[] COMMANDS = {
        "top", "player_name", "valet_name"
    };

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> available = new ArrayList<>();
        List<String> voidList = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], available, voidList);
            Collections.sort(available);
            return available;
        }
        if (args.length == 2);

        return voidList;
    }
}
