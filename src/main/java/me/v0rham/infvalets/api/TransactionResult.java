package me.v0rham.infvalets.api;

/** Результат операции с балансом. */
public enum TransactionResult {
    /** Операция выполнена. */
    SUCCESS,
    /** Такой валюты нет в config.yml. */
    UNKNOWN_VALET,
    /** Некорректная сумма (отрицательная, нулевая там, где это запрещено) или плагин отключён. */
    INVALID_AMOUNT,
    /** Недостаточно средств. */
    INSUFFICIENT_FUNDS,
    /** Итоговый баланс превысил бы лимит валюты. */
    LIMIT_REACHED,
    /** Перевод самому себе. */
    SAME_ACCOUNT,
    /** Операцию отменил другой плагин через {@link ValetTransactionEvent}. */
    CANCELLED,
    /** Ошибка базы данных (подробности в консоли сервера) или плагин отключён. */
    ERROR;

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
