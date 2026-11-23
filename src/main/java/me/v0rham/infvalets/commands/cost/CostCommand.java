package me.v0rham.infvalets.commands.cost;

import me.v0rham.infvalets.Main;
import me.v0rham.infvalets.api.Api;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** /costcommands <название> — списывает валюту и выполняет команды из секции CostCommands. */
public class CostCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Api.getMessage("Messages.Errors.only-players", true));
            return true;
        }
        if (!sender.hasPermission("infvalets.costcommands")) {
            sender.sendMessage(Api.getMessage("Messages.Errors.not-permission", true));
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(Api.getMessage("Messages.usage-costcommand", true));
            return true;
        }

        Main plugin = Main.getInstance();
        Player player = (Player) sender;

        ConfigurationSection section = findSection(args[0]);
        if (section == null) {
            sender.sendMessage(Api.getMessage("Messages.Errors.unknown-costcommand", true));
            return true;
        }

        long cost = section.getLong("cost", 0);
        String valet = section.contains("valet") ? plugin.findValet(section.getString("valet")) : plugin.getDefaultValet();
        if (valet == null) {
            plugin.getLogger().warning("CostCommands." + section.getName()
                    + ": не указана корректная валюта (ключ 'valet'), а валют в конфиге несколько");
            sender.sendMessage(Api.getMessage("Messages.Errors.database", true));
            return true;
        }

        try {
            if (cost > 0 && !plugin.getData().take(player.getUniqueId(), valet, cost)) {
                sender.sendMessage(Api.getMessage("Messages.Errors.not-enough", true));
                return true;
            }
        } catch (SQLException exception) {
            Api.dbError(sender, exception);
            return true;
        }

        boolean asPlayer = "PLAYER".equalsIgnoreCase(section.getString("type", "SERVER"));
        for (String raw : section.getStringList("commands")) {
            String line = Api.translateColor(raw.replace("$player", player.getName()));
            if (line.startsWith("/")) line = line.substring(1);
            if (asPlayer) {
                player.performCommand(line);
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line);
            }
        }

        sender.sendMessage(Api.getMessage("Messages.cost-paid", true,
                "%valet%", valet, "%amount%", Api.format(cost)));
        return true;
    }

    // Название команды ищем без учёта регистра
    private ConfigurationSection findSection(String name) {
        ConfigurationSection all = Main.getInstance().getConfig().getConfigurationSection("CostCommands");
        if (all == null) return null;
        for (String key : all.getKeys(false)) {
            if (key.equalsIgnoreCase(name)) return all.getConfigurationSection(key);
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        ConfigurationSection all = Main.getInstance().getConfig().getConfigurationSection("CostCommands");
        if (args.length == 1 && all != null) {
            StringUtil.copyPartialMatches(args[0], all.getKeys(false), result);
            Collections.sort(result);
        }
        return result;
    }
}
