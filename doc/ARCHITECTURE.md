# Архітектура: Crypto Exchange Simulation

Стек: **Java 21 · Spring Boot 3.2 · Spring Data JPA · Spring Security · WebSocket · React 19 (FSD)**.  
Мета: учбова CEX з коректним matching, резервами, комісіями та realtime UI.

---

## 1. Шари

```text
┌─────────────────────────────────────────────────────────┐
│  Web: Controllers · JWT Filter · WebSocketConfig        │
├─────────────────────────────────────────────────────────┤
│  Application: Order / Wallet / Account / MarketData /   │
│               Fee / PasswordReset / Arbitrage / Oracle  │
├──────────────────────┬──────────────────────────────────┤
│  Engine (in-memory)  │  Persistence (JPA / H2)          │
│  MatchingEngine      │  User, Wallet, Order, Candle…    │
│  OrderBook           │  Repositories + @Version         │
│  MatchResult         │                                  │
└──────────────────────┴──────────────────────────────────┘
```

Залежності строго вниз: controller → service → engine/model/repository.  
DTO + Mapper на межі API (без витоку JPA-сутностей назовні).

Детальні UML: [UML.md](UML.md).

---

## 2. Ключові сервіси

| Компонент | Роль |
|-----------|------|
| `OrderService` | Фасад ордерів: lock по символу, `TransactionTemplate`, matching, cancel, OCO |
| `MatchingManager` | In-memory стакани + пул trigger-ордерів; restore з БД на старті |
| `MatchingEngine` | Чистий розрахунок угод → `MatchResult` (без side-effects на book) |
| `WalletService` | Credit/debit/reserve/release + audit (`TransactionRecord`) |
| `FeeService` | Maker/taker; settlement у `SYSTEM_FEES` |
| `MarketDataService` | OHLCV, WS broadcast (book / trades / candles) |
| `AccountService` | Register, roles, change password |
| `JwtService` + filter | Bearer JWT (`userId`, `username`, `tokenVersion`) |
| `PasswordResetService` | One-time hashed token, інкремент `tokenVersion` |
| `ExternalPriceOracleService` | Котирування через `MarketDataQuoteClient` |
| `ArbitrageBotService` | Демо-бот на дельті oracle vs внутрішній ринок |

---

## 3. Lifecycle ордера

1. HTTP → валідація DTO (Jakarta Validation).
2. `synchronized` по ключу пари (`base/quote`) — **поза** транзакцією.
3. Усередині `TransactionTemplate`:
   - persist `PENDING`;
   - reserve у `Wallet`;
   - `MatchingEngine.match()`;
   - settle балансів + fees;
   - оновити статуси / filled.
4. `afterCommit`: застосувати дельти до `OrderBook`, WS publish.
5. Unlock.

**Чому так:** lock поза TX уникає утримання БД-з’єднання під час очікування; post-commit sync уникає «пам’ять попереду БД».

---

## 4. Concurrency & цілісність

| Механізм | Де |
|----------|----|
| Fine-grained lock per symbol | `OrderService.locks` |
| Optimistic locking | `@Version` на `User`, `Wallet`, `Order` |
| Virtual Threads | `spring.threads.virtual.enabled=true` |
| Thread-safe collections | `ConcurrentHashMap` / skip-list у book |
| Auth rate limit | in-memory `AuthRateLimiter` → 429 |

---

## 5. Типи ордерів

| Тип | Поведінка |
|-----|-----------|
| `LIMIT` | У стакан; match якщо ціна перетинає протилежну сторону |
| `MARKET` | Негайний match; опційний `priceLimit` (slippage) |
| `STOP_LOSS` / `TAKE_PROFIT` | У trigger-пул; активація за last trade price |
| `OCO` | Дві linked legs, один reserve; друга скасовується після активації першої |

FIFO на одному ціновому рівні.

---

## 6. Frontend (FSD)

```text
frontend/src/
  app/          # shell, layout
  pages/        # login, register, dashboard, admin, reset
  widgets/      # OrderBook, Chart, Portfolio, TradingForm…
  features/     # place-order
  entities/     # user / auth store
  shared/api/   # axios + 401 logout, React Query
```

Realtime: SockJS + STOMP → ті самі топики, що публікує `MarketDataService`.

---

## 7. Зовнішні залежності

- **`fin-core`** (`com.alex.fin`): `Price`, `Quantity`, `InstrumentId`, `CurrencyCode` + JPA converters.
- **Market-data service** (опційно): `MARKET_DATA_BASE_URL`; без нього oracle/bot деградують gracefully за конфігом клієнта.

---

## 8. Нецілі (out of scope)

Реальні депозити/вивід, KYC, мультирегіон, Kafka-bus, production HA matching cluster, cold wallet.
Це симуляція для портфоліо та рев’ю алгоритмів/архітектури.
