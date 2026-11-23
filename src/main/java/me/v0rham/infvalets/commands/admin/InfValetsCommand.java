package me.v0rham.infvalets.commands.admin;

import me.v0rham.infvalets.Main;
import me.v0rham.infvalets.api.Api;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** /infvalets reload — перечитать config.yml. Право: infvalets.admin. */
public class InfValetsCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("infvalets.admin")) {
            sender.sendMessage(Api.getMessage("Messages.Errors.not-permission", true));
            return true;
        }
        if (args.length == 1 && args[0].toLowerCase(Locale.ROOT).equals("reload")) {
            if (Main.getInstance().reloadAll()) {
                sender.sendMessage(Api.getMessage("Messages.reload-done", true));
            } else {
                sender.sendMessage(Api.getMessage("Messages.Errors.database", true));
            }
            return true;
        }
        sender.sendMessage(Api.getMessage("Messages.usage-infvalets", true));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("infvalets.admin") && "reload".startsWith(args[0].toLowerCase(Locale.ROOT))) {
            result.add("reload");
        }
        return Collections.unmodifiableList(result);
    }
}
