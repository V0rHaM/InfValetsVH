package me.v0rham.infvalets.hook;

import me.v0rham.infvalets.api.Api;
import me.v0rham.infvalets.api.InfValetsAPI;
import me.v0rham.infvalets.api.TransactionResult;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.OfflinePlayer;

import java.util.Collections;
import java.util.List;

/**
 * Мост Vault -> InfValets для одной выбранной валюты.
 * Банки не поддерживаются, дробные суммы не поддерживаются (баланс целочисленный).
 */
public class VaultEconomy implements Economy {

    // Максимум, который double ещё хранит без потери точности (2^53)
    private static final double MAX_AMOUNT = 9.0e15;

    private final InfValetsAPI api;
    private final String valet;
    private final String nameSingular;
    private final String namePlural;

    public VaultEconomy(InfValetsAPI api, String valet, String nameSingular, String namePlural) {
        this.api = api;
        this.valet = valet;
        this.nameSingular = nameSingular;
        this.namePlural = namePlural;
    }

    // ------------------------------------------------------------ общее

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "InfValets";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 0;
    }

    @Override
    public String format(double amount) {
        long value = (long) amount;
        return Api.format(value) + " " + (value == 1 ? nameSingular : namePlural);
    }

    @Override
    public String currencyNamePlural() {
        return namePlural;
    }

    @Override
    public String currencyNameSingular() {
        return nameSingular;
    }

    // ------------------------------------------------------------ аккаунты

    // Записи создаются лениво при первом начислении, поэтому аккаунт есть у любого игрока
    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return true;
    }

    @Override
    public boolean hasAccount(String playerName) {
        return Api.resolvePlayer(playerName) != null;
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    // ------------------------------------------------------------ баланс

    @Override
    public double getBalance(OfflinePlayer player) {
        long balance = api.getBalance(player.getUniqueId(), valet);
        return balance < 0 ? 0 : balance;
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public double getBalance(String playerName) {
        OfflinePlayer player = Api.resolvePlayer(playerName);
        return player == null ? 0 : getBalance(player);
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        if (!validAmount(amount)) return false;
        return api.has(player.getUniqueId(), valet, (long) amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public boolean has(String playerName, double amount) {
        OfflinePlayer player = Api.resolvePlayer(playerName);
        return player != null && has(player, amount);
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    // ------------------------------------------------------------ операции

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        return change(player, amount, false);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return change(player, amount, false);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return change(Api.resolvePlayer(playerName), amount, false);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        return change(player, amount, true);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return change(player, amount, true);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return change(Api.resolvePlayer(playerName), amount, true);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    private EconomyResponse change(OfflinePlayer player, double amount, boolean deposit) {
        if (player == null) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Игрок не найден");
        }
        double current = getBalance(player);
        if (!validAmount(amount)) {
            return new EconomyResponse(0, current, ResponseType.FAILURE,
                    "Сумма должна быть целым неотрицательным числом");
        }

        long value = (long) amount;
        if (value == 0) { // нулевая операция: ничего не меняем
            return new EconomyResponse(0, current, ResponseType.SUCCESS, null);
        }

        TransactionResult result = deposit
                ? api.deposit(player.getUniqueId(), player.getName(), valet, value)
                : api.withdraw(player.getUniqueId(), valet, value);

        double after = getBalance(player);
        if (result.isSuccess()) {
            return new EconomyResponse(amount, after, ResponseType.SUCCESS, null);
        }
        return new EconomyResponse(0, after, ResponseType.FAILURE, describe(result));
    }

    private static boolean validAmount(double amount) {
        return !Double.isNaN(amount) && !Double.isInfinite(amount)
                && amount >= 0 && amount <= MAX_AMOUNT && Math.floor(amount) == amount;
    }

    private static String describe(TransactionResult result) {
        switch (result) {
            case INSUFFICIENT_FUNDS:
                return "Недостаточно средств";
            case LIMIT_REACHED:
                return "Превышен лимит валюты";
            case CANCELLED:
                return "Операция отменена";
            case INVALID_AMOUNT:
                return "Некорректная сумма";
            default:
                return "Ошибка InfValets";
        }
    }

    // ------------------------------------------------------------ банки (не поддерживаются)

    private static EconomyResponse noBanks() {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "InfValets не поддерживает банки");
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return noBanks();
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return noBanks();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return noBanks();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return noBanks();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return noBanks();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return noBanks();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return noBanks();
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return noBanks();
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return noBanks();
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return noBanks();
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return noBanks();
    }

    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }
}
