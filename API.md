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

## Плейсхолдеры (PlaceholderAPI)

`%infvalets_balance_<валюта>%`
