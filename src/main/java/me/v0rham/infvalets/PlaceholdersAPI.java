package me.v0rham.infvalets;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class PlaceholdersAPI extends PlaceholderExpansion {

    @Override
    public String getAuthor() {
        return "V0rHaM";
    }

    @Override
    public String getIdentifier() {
        return Main.instance.getDescription().getName();
    }

    @Override
    public String getVersion() {
        return Main.instance.getDescription().getVersion();
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {

//        %infvalets_balance_<valet_name>%

//        File playerFile = Api.getFileSeparator("Player", player.getName().toLowerCase());
//
//        if (params.equalsIgnoreCase("clan")) {
//            if (playerFile.exists()) {
//                String startName = Api.getConfigString("Default.Brackets.start");
//                String endName = Api.getConfigString("Default.Brackets.end");
//                String name = Api.loadYaml(playerFile).getString("Player.Clan");
//                return Api.translateColor(startName + name + endName);
//            }
//            return "";
//        }
//        if (params.equalsIgnoreCase("role")) {
//            if (playerFile.exists()) {
//                return Api.translateColor(Api.loadYaml(playerFile).getString("Player.Role"));
//            }
//            return "";
//        }
//        if (params.equalsIgnoreCase("shortname")) {
//            if (playerFile.exists()) {
//                File clanFile = Api.getClanFile(Api.getClanNameInPlayerFile(player.getName(), false));
//                if (clanFile.exists()) {
//                    String startName = Api.getConfigString("Default.Brackets.start");
//                    String endName = Api.getConfigString("Default.Brackets.end");
//                    String shortname = Api.loadYaml(clanFile).getString("Clan.ShortName");
//                    return Api.translateColor(startName + shortname + endName);
//                }
//            }
//            return "";
//        }
        return null;
    }
}