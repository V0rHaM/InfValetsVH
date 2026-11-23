package me.v0rham.infvalets.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Вызывается ПОСЛЕ успешного изменения баланса (отменить нельзя).
 * Актуальный баланс можно получить через {@link InfValetsAPI#getBalance(UUID, String)}.
 * Событие может быть асинхронным — проверяйте {@link #isAsynchronous()}.
 */
public class ValetBalanceChangedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final TransactionType type;
    private final UUID target;
    private final UUID source;
    private final String valet;
    private final long amount;

    public ValetBalanceChangedEvent(boolean async, TransactionType type, UUID target, UUID source,
                                    String valet, long amount) {
        super(async);
        this.type = type;
        this.target = target;
        this.source = source;
        this.valet = valet;
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    /** Игрок, чей баланс изменился (при переводе — получатель). */
    public UUID getTarget() {
        return target;
    }

    /** Отправитель при переводе, иначе null. */
    public UUID getSource() {
        return source;
    }

    public String getValet() {
        return valet;
    }

    public long getAmount() {
        return amount;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
