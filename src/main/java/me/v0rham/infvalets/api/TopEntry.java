package me.v0rham.infvalets.api;

/** Строка топа: ник игрока (последний известный) и его баланс. */
public final class TopEntry {

    private final String name;
    private final long balance;

    public TopEntry(String name, long balance) {
        this.name = name;
        this.balance = balance;
    }

    public String name() {
        return name;
    }

    public long balance() {
        return balance;
    }

    @Override
    public String toString() {
        return name + "=" + balance;
    }
}
