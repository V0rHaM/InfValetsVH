package me.v0rham.infvalets;

import me.v0rham.infvalets.database.DBConnect;
import me.v0rham.infvalets.database.DBUpdate;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    public static Main instance;
    public static DBConnect SQL = new DBConnect();
    public static DBUpdate data = new DBUpdate();

    @Override
    public void onEnable() {
        if (instance == null) instance = this;
        if (this.getServer().getPluginManager().isPluginEnabled("Vault")) {
            this.getServer().getLogger().info("Vault connected!");
        }
    }

    @Override
    public void onDisable() {
        if (instance != null) instance = null;
    }

    public static Main getInstance() {
        return instance;
    }
}
