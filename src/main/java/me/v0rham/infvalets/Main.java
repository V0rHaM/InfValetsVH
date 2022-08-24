package me.v0rham.infvalets;

import me.v0rham.infvalets.commands.balance.Balance;
import me.v0rham.infvalets.commands.balance.BalanceTabComplete;
import me.v0rham.infvalets.database.DBConnect;
import me.v0rham.infvalets.database.DBUpdate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    public static Main instance;
    public static DBConnect SQL = new DBConnect();
    public static DBUpdate data = new DBUpdate();

    @Override
    public void onEnable() {
        if (instance == null) instance = this;
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")){
            new PlaceholdersAPI().register();
            getLogger().info(ChatColor.GREEN + "PlaceholderAPI has successful hooked.");
        }
        if (this.getServer().getPluginManager().isPluginEnabled("Vault")) {
            this.getServer().getLogger().info(ChatColor.GREEN + "Vault connected!");
        }
        saveDefaultConfig();
        this.getCommand("balance").setExecutor(new Balance());
        this.getCommand("balance").setTabCompleter(new BalanceTabComplete());
        if (SQL.isConnected()) {
            for (String valetName : this.getConfig().getConfigurationSection("Valet").getKeys(false)) {
                if (valetName != null) {
                    DBUpdate.createTable(valetName);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        if (instance != null) instance = null;
        SQL.disconnect();
        if (!SQL.isConnected()) {
            this.getLogger().info(ChatColor.GREEN + "Database disconnected!");
        }
    }

    public static Main getInstance() {
        return instance;
    }
}
