package me.v0rham.infvalets.api;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Публичное API InfValets (аналог Vault для нескольких валют).
 *
 * <pre>{@code
 * InfValetsAPI api = InfValetsAPI.get();
 * if (api != null && api.withdraw(player.getUniqueId(), "coin", 100).isSuccess()) {
 *     // списали 100 coin
 * }
 * }</pre>
 *
 * Все методы потокобезопасны и не бросают исключений, их можно вызывать и из асинхронных задач.
 * Имя валюты не чувствительно к регистру. Суммы — целые, в минимальных единицах валюты.
 */
public interface InfValetsAPI {

    /** Возвращает API или null, если InfValets не установлен / не запущен. */
    static InfValetsAPI get() {
        RegisteredServiceProvider<InfValetsAPI> provider =
                Bukkit.getServicesManager().getRegistration(InfValetsAPI.class);
        return provider == null ? null : provider.getProvider();
    }

    /** Все валюты из config.yml (неизменяемая коллекция). */
    Collection<String> getValets();

    /** Имя валюты в том виде, как оно записано в config.yml, или null, если такой валюты нет. */
    String findValet(String name);

    /** Лимит валюты на одного игрока или -1, если валюты нет. */
    long getLimit(String valet);

    /** Баланс игрока (0, если записи ещё нет). -1 — валюта неизвестна или ошибка БД. */
    long getBalance(UUID uuid, String valet);

    /** Хватает ли игроку amount. При ошибке или неизвестной валюте — false. */
    boolean has(UUID uuid, String valet, long amount);

    /** Начислить amount (&gt; 0). playerName можно передать null. */
    TransactionResult deposit(UUID uuid, String playerName, String valet, long amount);

    /** Списать amount (&gt; 0). Атомарно: баланс не уйдёт в минус. */
    TransactionResult withdraw(UUID uuid, String valet, long amount);

    /** Установить баланс (amount &gt;= 0). playerName можно передать null. */
    TransactionResult set(UUID uuid, String playerName, String valet, long amount);

    /** Атомарный перевод между игроками: либо выполнится целиком, либо ничего не изменится. */
    TransactionResult transfer(UUID from, UUID to, String toName, String valet, long amount);

    /** Топ по валюте (по убыванию). Пустой список при ошибке или неизвестной валюте. */
    List<TopEntry> getTop(String valet, int size);

    // ---- конвертация валют ----

    /** Текущий курс (сколько «в» за 1 «из», без комиссии) или -1, если обмен недоступен. */
    double getRate(String fromValet, String toValet);

    /** Расчёт обмена без выполнения: сколько спишется и сколько будет получено. Баланс не проверяется. */
    ConversionResult quote(String fromValet, String toValet, long amount);

    /**
     * Обмен валюты по курсу из config.yml (FIXED или DYNAMIC). Атомарный.
     * Вызывает ValetTransactionEvent для списания и для зачисления.
     */
    ConversionResult convert(UUID uuid, String playerName, String fromValet, String toValet, long amount);

    // ---- удобные перегрузки для OfflinePlayer ----

    default long getBalance(OfflinePlayer player, String valet) {
        return getBalance(player.getUniqueId(), valet);
    }

    default boolean has(OfflinePlayer player, String valet, long amount) {
        return has(player.getUniqueId(), valet, amount);
    }

    default TransactionResult deposit(OfflinePlayer player, String valet, long amount) {
        return deposit(player.getUniqueId(), player.getName(), valet, amount);
    }

    default TransactionResult withdraw(OfflinePlayer player, String valet, long amount) {
        return withdraw(player.getUniqueId(), valet, amount);
    }
}
