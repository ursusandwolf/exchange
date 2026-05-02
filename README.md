# Учебная модель криптобиржи (Crypto Exchange Simulation)

Примитивная in-memory реализация криптобиржи для учебных целей. Без реальных денег, API и блокчейна.

## Структура пакетов

```
com.exchange/
├── enums/
│   ├── Side.java          # BUY, SELL
│   ├── OrderType.java     # LIMIT, MARKET
│   └── OrderStatus.java   # PENDING, PARTIALLY_FILLED, FILLED, CANCELLED, REJECTED
├── model/
│   ├── User.java          # Пользователь с кошельком
│   ├── Wallet.java        # Кошелёк с балансами и резервированием
│   ├── Order.java         # Заявка на покупку/продажу
│   └── Trade.java         # Совершённая сделка
├── engine/
│   ├── OrderBook.java     # Стакан заявок для одной торговой пары
│   └── MatchingEngine.java # Движок сведения ордеров
├── service/
│   └── ExchangeService.java # Основной сервис биржи
└── ExchangeDemo.java      # Демо-сценарий использования
```

## Основные сущности

### Enum'ы

| Enum | Значения | Описание |
|------|----------|----------|
| `Side` | BUY, SELL | Направление ордера |
| `OrderType` | LIMIT, MARKET | Тип ордера |
| `OrderStatus` | PENDING, PARTIALLY_FILLED, FILLED, CANCELLED, REJECTED | Статус ордера |

### Модельные классы

- **User** — пользователь биржи с уникальным ID и кошельком
- **Wallet** — хранит балансы по нескольким активам, поддерживает резервирование средств
- **Order** — заявка с полями: userId, baseAsset, quoteAsset, side, type, quantity, price
- **Trade** — результат сделки между buy и sell ордерами

### Движки

- **OrderBook** — стакан заявок для пары типа BTC/USDT
  - Bids (покупки) сортируются по убыванию цены
  - Asks (продажи) сортируются по возрастанию цены
  - FIFO внутри одной цены
  
- **MatchingEngine** — алгоритм сведения ордеров
  - Если цена BUY >= лучшей цены SELL → сделка исполняется
  - MARKET ордера исполняются по лучшей доступной цене
  - Частичное исполнение разрешено

### Сервис

- **ExchangeService** — координирует работу:
  1. Принимает ордер от пользователя
  2. Валидирует баланс
  3. Резервирует средства
  4. Помещает ордер в стакан
  5. Вызывает matching engine
  6. Возвращает список совершённых сделок
  7. Исполняет расчёты между кошельками

## Быстрый старт

### Компиляция

```bash
cd /workspace
javac -d target/classes $(find src/main/java -name "*.java")
```

### Запуск демо

```bash
java -cp target/classes com.exchange.ExchangeDemo
```

## Пример сценария

```java
// 1. Создаём сервис
ExchangeService exchange = new ExchangeService();

// 2. Регистрируем пользователей
User alice = exchange.registerUser("Alice");
User bob = exchange.registerUser("Bob");

// 3. Пополняем кошельки (тестовые активы)
alice.deposit("USDT", new BigDecimal("100000"));
bob.deposit("BTC", new BigDecimal("5"));

// 4. Bob выставляет LIMIT SELL ордер
exchange.submitOrder(
    bob, "BTC", "USDT", 
    Side.SELL, 
    new BigDecimal("2"), 
    new BigDecimal("45000")
);

// 5. Alice выставляет LIMIT BUY ордер — сделка исполняется!
List<Trade> trades = exchange.submitOrder(
    alice, "BTC", "USDT", 
    Side.BUY, 
    new BigDecimal("1"), 
    new BigDecimal("45000")
);

// 6. Проверяем результат
for (Trade trade : trades) {
    System.out.println(trade);
}
```

## Точки расширения (TODO)

В коде помечены комментариями `// TODO:` места для будущего расширения:

### Комиссии
- `Trade.java` — поля `buyerFee`, `sellerFee` (сейчас = 0)
- `ExchangeService.settleTrade()` — удержание комиссий перед зачислением
- `MatchingEngine.determineTradePrice()` — maker/taker pricing

### Лимиты риска
- `ExchangeService.submitOrder()` — валидация мин/макс размера ордера
- `ExchangeService.validateAndReserve()` — проверка exposure limits
- `User.deposit()` — лимиты на пополнение

### Persistency (БД)
- `ExchangeService.submitOrder()` — сохранение ордера до исполнения
- `ExchangeService.cancelOrder()` — запись события отмены
- `Wallet` — аудит всех транзакций
- Снэпшоты состояния стакана для восстановления

### Дополнительные возможности
- TimeInForce (GTC, IOC, FOK) в `Order`
- Stop-loss / take-profit уровни
- WebSocket/Kafka уведомления
- Глобальный реестр ордеров для отмены

## Архитектурные решения

1. **Thread-safety**: Использованы `ConcurrentHashMap`, `ConcurrentSkipListMap` и `synchronized` методы
2. **BigDecimal**: Все денежные расчёты через `BigDecimal` для точности
3. **FIFO**: Ордера с одинаковой ценой исполняются в порядке поступления
4. **Резервирование**: Средства резервируются при выставлении ордера, списываются после сделки

## Лицензия

Учебный проект, свободное использование.
