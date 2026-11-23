package me.v0rham.infvalets;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.sql.SQLException;
import java.util.Locale;

public class PlaceholdersAPI extends PlaceholderExpansion {

    private final Main plugin;

    public PlaceholdersAPI(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getIdentifier() {
        return "infvalets";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    // %infvalets_balance_<valet_name>%
    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";

        String key = params.toLowerCase(Locale.ROOT);
        if (key.startsWith("balance_")) {
            String valet = plugin.findValet(key.substring("balance_".length()));
            if (valet == null) return "";
            try {
                return String.valueOf(plugin.getData().getBalance(player.getUniqueId(), valet));
            } catch (SQLException exception) {
                return "";
            }
        }
        return null;
    }
}
