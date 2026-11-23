package me.v0rham.infvalets.hook;

import me.v0rham.infvalets.Main;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.ServicePriority;

import java.util.Iterator;

/**
 * Регистрация Economy-провайдера для Vault. Вынесена в отдельный класс, чтобы без Vault
 * на сервере классы Vault вообще не загружались (Main обращается сюда только если Vault найден).
 */
public final class VaultHook {

    private VaultHook() {
    }

    public static void register(Main plugin) {
        String valet = plugin.findValet(plugin.getConfig().getString("Vault.valet"));
        if (valet == null) {
            Iterator<String> all = plugin.getValets().iterator();
            if (!all.hasNext()) return;
            valet = all.next();
            plugin.getLogger().warning("Vault.valet не задан или указан неверно, используется валюта '" + valet + "'");
        }

        VaultEconomy economy = new VaultEconomy(plugin.getApi(), valet,
                plugin.getConfig().getString("Vault.name-singular", valet),
                plugin.getConfig().getString("Vault.name-plural", valet));

        plugin.getServer().getServicesManager().register(Economy.class, economy, plugin, ServicePriority.Normal);
        plugin.getLogger().info("Vault Economy provider registered (валюта: " + valet + ")");
    }
}
