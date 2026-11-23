package me.v0rham.infvalets;

import me.v0rham.infvalets.api.InfValetsAPI;
import me.v0rham.infvalets.api.InfValetsAPIImpl;
import me.v0rham.infvalets.commands.admin.InfValetsCommand;
import me.v0rham.infvalets.commands.balance.BalTop;
import me.v0rham.infvalets.commands.balance.Balance;
import me.v0rham.infvalets.commands.balance.BalanceTabComplete;
import me.v0rham.infvalets.commands.cost.CostCommand;
import me.v0rham.infvalets.commands.eco.Eco;
import me.v0rham.infvalets.commands.pay.Pay;
import me.v0rham.infvalets.hook.VaultHook;
import me.v0rham.infvalets.database.DBConnect;
import me.v0rham.infvalets.database.DBUpdate;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class Main extends JavaPlugin {

    private static final Pattern VALET_NAME = Pattern.compile("[A-Za-z0-9_]{1,32}");

    private static Main instance;

    private DBConnect database;
    private DBUpdate data;
    private InfValetsAPIImpl api;
    // имя в нижнем регистре -> имя как в config.yml
    private final Map<String, String> valets = new LinkedHashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        loadValets();
        if (valets.isEmpty()) {
            getLogger().severe("В config.yml нет ни одной корректной валюты (секция Valet). Плагин отключён.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        database = new DBConnect(this);
        data = new DBUpdate(database);
        try {
            database.connect();
            for (String valet : valets.values()) {
                data.createTable(valet);
            }
            getLogger().info("Database connected!");
        } catch (SQLException exception) {
            getLogger().log(Level.SEVERE, "Не удалось подключиться к базе данных. Плагин отключён.", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Публичное API: другие плагины получают его через InfValetsAPI.get()
        api = new InfValetsAPIImpl(this);
        getServer().getServicesManager().register(InfValetsAPI.class, api, this, ServicePriority.Normal);

        registerCommand("balance", new Balance(), new BalanceTabComplete());
        Pay pay = new Pay();
        registerCommand("pay", pay, pay);
        CostCommand cost = new CostCommand();
        registerCommand("costcommands", cost, cost);
        Eco eco = new Eco();
        registerCommand("eco", eco, eco);
        BalTop balTop = new BalTop();
        registerCommand("baltop", balTop, balTop);
        InfValetsCommand admin = new InfValetsCommand();
        registerCommand("infvalets", admin, admin);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholdersAPI(this).register();
            getLogger().info("PlaceholderAPI has successful hooked.");
        }
        // Vault не обязателен: если он есть, другие плагины смогут работать с InfValets через него
        if (getConfig().getBoolean("Vault.enabled", true) && Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            VaultHook.register(this);
        }

        new Metrics(this, 35530);
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        if (database != null) {
            database.disconnect();
            getLogger().info("Database disconnected!");
        }
        instance = null;
    }

    /** Перечитывает config.yml и создаёт таблицы для новых валют (смена Database.url требует перезапуска). */
    public boolean reloadAll() {
        reloadConfig();
        loadValets();
        try {
            for (String valet : valets.values()) {
                data.createTable(valet);
            }
            return true;
        } catch (SQLException exception) {
            getLogger().log(Level.SEVERE, "Не удалось создать таблицы после перезагрузки", exception);
            return false;
        }
    }

    private void registerCommand(String name, CommandExecutor executor, TabCompleter completer) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("Команда '" + name + "' не описана в plugin.yml");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(completer);
    }

    private void loadValets() {
        valets.clear();
        ConfigurationSection section = getConfig().getConfigurationSection("Valet");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            if (!VALET_NAME.matcher(key).matches()) {
                getLogger().warning("Валюта '" + key + "' пропущена: допустимы только латиница, цифры и _");
                continue;
            }
            valets.put(key.toLowerCase(Locale.ROOT), key);
        }
    }

    /** Имя валюты как в config.yml (регистр не важен) или null, если такой нет. */
    public String findValet(String input) {
        return input == null ? null : valets.get(input.toLowerCase(Locale.ROOT));
    }

    public Collection<String> getValets() {
        return Collections.unmodifiableCollection(valets.values());
    }

    /** Единственная валюта, если она одна; иначе null (тогда валюту надо указывать явно). */
    public String getDefaultValet() {
        return valets.size() == 1 ? valets.values().iterator().next() : null;
    }

    public long getLimit(String valet) {
        return getConfig().getLong("Valet." + valet + ".limit", Long.MAX_VALUE / 2);
    }

    public DBUpdate getData() {
        return data;
    }

    public InfValetsAPIImpl getApi() {
        return api;
    }

    public static Main getInstance() {
        return instance;
    }
}
