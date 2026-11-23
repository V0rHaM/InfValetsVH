# InfValets API для разработчиков

InfValets — самостоятельная мультивалютная экономика. Vault не нужен: другие плагины
работают с ней напрямую через сервис `InfValetsAPI`.

## Подключение

1. Добавьте `InfValets.jar` как `provided`/`compileOnly` зависимость (Java 17+).
2. В `plugin.yml` своего плагина укажите `depend: [InfValets]` (или `softdepend`).

## Использование

```java
InfValetsAPI api = InfValetsAPI.get();   // null, если InfValets не запущен
if (api == null) return;

long coins = api.getBalance(player, "coin");          // -1 при ошибке / неизвестной валюте

TransactionResult r = api.withdraw(player, "coin", 100);
if (r.isSuccess()) {
    // выдаём товар
} else if (r == TransactionResult.INSUFFICIENT_FUNDS) {
    player.sendMessage("Не хватает монет!");
}

api.deposit(player, "coin", 50);
api.transfer(fromUuid, toUuid, "Steve", "coin", 25);
api.getTop("coin", 10);                                // List<TopEntry>
```

* Имя валюты не чувствительно к регистру, список — `api.getValets()`.
* Методы не бросают исключений, потокобезопасны и могут вызываться из асинхронных задач.
* Списание и перевод атомарны: баланс не уйдёт в минус и не превысит лимит валюты.

## События

* `ValetTransactionEvent` — ДО изменения баланса, можно отменить (`setCancelled(true)`);
  тогда операция вернёт `TransactionResult.CANCELLED`.
* `ValetBalanceChangedEvent` — ПОСЛЕ успешного изменения (отменить нельзя).

Типы операций: `DEPOSIT`, `WITHDRAW`, `SET`, `TRANSFER` (при переводе `getSource()` — отправитель,
`getTarget()` — получатель). События вызываются и для команд самого плагина
(`/pay`, `/balance give|take|set`, `/costcommands`).

Если API вызвали не из главного потока, события асинхронные — проверяйте `event.isAsynchronous()`.

```java
@EventHandler
public void onTx(ValetTransactionEvent e) {
    if (e.getValet().equals("coin") && e.getAmount() > 1_000_000) e.setCancelled(true);
}
```

## Плейсхолдеры (PlaceholderAPI)

`%infvalets_balance_<валюта>%`
