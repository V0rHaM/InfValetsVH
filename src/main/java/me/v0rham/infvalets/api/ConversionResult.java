package me.v0rham.infvalets.api;

/** Результат конвертации (или расчёта курса через quote). */
public final class ConversionResult {

    private final TransactionResult status;
    private final long spent;
    private final long received;
    private final double rate;

    public ConversionResult(TransactionResult status, long spent, long received, double rate) {
        this.status = status;
        this.spent = spent;
        this.received = received;
        this.rate = rate;
    }

    public static ConversionResult failure(TransactionResult status) {
        return new ConversionResult(status, 0, 0, 0);
    }

    public TransactionResult getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return status.isSuccess();
    }

    /** Сколько списано (валюты «из»). */
    public long getSpent() {
        return spent;
    }

    /** Сколько получено (валюты «в») с учётом комиссии. */
    public long getReceived() {
        return received;
    }

    /** Курс без учёта комиссии: сколько единиц «в» даётся за 1 единицу «из». */
    public double getRate() {
        return rate;
    }
}
