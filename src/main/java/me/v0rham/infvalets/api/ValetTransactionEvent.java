package me.v0rham.infvalets.api;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Вызывается ДО изменения баланса. Если отменить, баланс не изменится,
 * а операция вернёт {@link TransactionResult#CANCELLED}.
 * <p>
 * Событие может быть асинхронным (если API вызвали не из главного потока) —
 * проверяйте {@link #isAsynchronous()}.
 */
public class ValetTransactionEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final TransactionType type;
    private final UUID target;
    private final UUID source;
    private final String valet;
    private final long amount;
    private boolean cancelled;

    public ValetTransactionEvent(boolean async, TransactionType type, UUID target, UUID source,
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

    /** Игрок, чей баланс меняется (при переводе — получатель). */
    public UUID getTarget() {
        return target;
    }

    /** Отправитель при переводе, иначе null. */
    public UUID getSource() {
        return source;
    }

    /** Имя валюты как в config.yml. */
    public String getValet() {
        return valet;
    }

    /** Сумма операции (для SET — новый баланс). */
    public long getAmount() {
        return amount;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
