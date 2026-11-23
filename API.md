# InfValets API для разработчиков

InfValets — самостоятельная мультивалютная экономика. Vault не нужен: другие плагины
работают с ней напрямую через сервис `InfValetsAPI`.

## Совместимость

* Minecraft **1.13 – 26.x** (Spigot, Paper и форки), одна и та же jar-сборка.
* Сборка идёт против API 1.16.5 и Java 8 — это сознательно, ради обратной совместимости.
  На Paper 26.x сервер требует Java 25, плагину это не мешает.
* Автозагрузка драйвера PostgreSQL (`libraries:` в plugin.yml) работает с 1.16.5; на более старых
  серверах используйте SQLite (она встроена в сервер).
* Плагин использует только публичное Bukkit API (без NMS), поэтому смена версий Minecraft
  ему не страшна.

## Подключение

1. Добавьте `InfValets.jar` как `provided`/`compileOnly` зависимость.
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

## Vault

Vault не нужен для работы плагина. Если он установлен, InfValets регистрирует `Economy`-провайдер
для одной валюты (секция `Vault` в config.yml), и плагины, которые знают только Vault
(магазины, `/baltop` и т.п.), работают с InfValets без доработок.
Ограничения: банки не поддерживаются, суммы только целые. Для нескольких валют используйте `InfValetsAPI`.

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
