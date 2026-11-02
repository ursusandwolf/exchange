# UML: Crypto Exchange Simulation

Діаграми відповідають коду на гілці `main` (`com.exchange.*`).  
Рендер: GitHub / будь-який Mermaid viewer.

---

## 1. Пакетна структура (логічні модулі)

```mermaid
flowchart TB
  subgraph web [Web]
    C[controller]
    CFG[config / security / ws]
  end
  subgraph app [Application]
    S[service]
    M[mapper]
    D[dto]
  end
  subgraph domain [Domain / Engine]
    E[engine]
    MOD[model]
    EN[enums]
  end
  subgraph infra [Infrastructure]
    R[repository]
    CL[client]
    X[exception]
  end
  C --> S
  C --> D
  S --> E
  S --> MOD
  S --> R
  S --> CL
  E --> MOD
  E --> EN
  MOD --> EN
  R --> MOD
  M --> MOD
  M --> D
  CFG --> S
```

---

## 2. Діаграма класів — Domain & Engine

```mermaid
classDiagram
  direction TB

  class User {
    -String id
    -String username
    -String email
    -String password
    -boolean admin
    -int tokenVersion
    -Long version
    -Wallet wallet
    +deposit(asset, amount)
  }

  class Wallet {
    -String ownerId
    -Map~String,BigDecimal~ balances
    -Map~String,BigDecimal~ reserved
    -Long version
    +credit(asset, amount)
    +debit(asset, amount)
    +reserve(asset, amount)
    +release(asset, amount)
    +available(asset) BigDecimal
  }

  class Order {
    -String id
    -String userId
    -InstrumentId baseAsset
    -InstrumentId quoteAsset
    -Side side
    -OrderType type
    -Quantity quantity
    -Price price
    -Price priceLimit
    -Price triggerPrice
    -String ocoGroupId
    -OrderStatus status
    -Quantity filledQuantity
    -BigDecimal reservedAmount
    -Long version
    +getRemainingQuantity() BigDecimal
    +isActive() boolean
  }

  class Trade {
    -String id
    -String symbol
    -Order buyOrder
    -Order sellOrder
    -Price price
    -Quantity quantity
    -BigDecimal totalAmount
    -BigDecimal buyerFee
    -BigDecimal sellerFee
    -String takerOrderId
    -String makerOrderId
  }

  class Candle {
    -String id
    -String symbol
    -Instant openTime
    -BigDecimal open
    -BigDecimal high
    -BigDecimal low
    -BigDecimal close
    -BigDecimal volume
  }

  class TransactionRecord {
    -String id
    -String userId
    -String asset
    -BigDecimal amount
    -String type
    -Instant createdAt
  }

  class PasswordResetToken {
    -String id
    -String userId
    -String tokenHash
    -Instant expiresAt
    -boolean used
  }

  class Side {
    <<enumeration>>
    BUY
    SELL
  }

  class OrderType {
    <<enumeration>>
    LIMIT
    MARKET
    STOP_LOSS
    TAKE_PROFIT
  }

  class OrderStatus {
    <<enumeration>>
    PENDING
    PARTIALLY_FILLED
    FILLED
    CANCELLED
    REJECTED
  }

  class OrderBook {
    -String symbol
    -NavigableMap bids
    -NavigableMap asks
    +add(Order)
    +remove(Order)
    +bestBid() Order
    +bestAsk() Order
    +hasMatchingOrders(Order) boolean
  }

  class MatchingEngine {
    -FeeService feeService
    +match(OrderBook, Order) MatchResult
  }

  class MatchResult {
    -List~Trade~ trades
    -List~Order~ ordersToRemove
    -Order remainingOrder
  }

  User "1" *-- "1" Wallet : owns
  Order --> Side
  Order --> OrderType
  Order --> OrderStatus
  Trade --> Order : buy / sell
  OrderBook "1" o-- "*" Order : levels
  MatchingEngine ..> OrderBook : reads
  MatchingEngine ..> MatchResult : creates
  MatchingEngine ..> Trade : creates
  MatchingEngine --> FeeService : fees
```

---

## 3. Діаграма класів — Services & Controllers

```mermaid
classDiagram
  direction LR

  class OrderController
  class AuthController
  class UserController
  class AdminController
  class MarketDataController
  class TradingViewController

  class OrderService {
    -Map locks
    -MatchingEngine matchingEngine
    +submitOrder(User, OrderRequest) List~Trade~
    +submitOco(User, OcoOrderRequest) OcoOrderResponse
    +cancelOrder(userId, orderId)
  }

  class MatchingManager {
    +getOrCreateBook(symbol) OrderBook
    +addTriggeredOrder(Order)
    +restoreFromDb()
  }

  class WalletService {
    +reserve(...)
    +settleTrade(...)
    +releaseReserve(...)
  }

  class AccountService
  class FeeService
  class MarketDataService
  class JwtService
  class PasswordResetService
  class AuthRateLimiter
  class ExternalPriceOracleService
  class ArbitrageBotService
  class MarketDataQuoteClient {
    <<interface>>
  }
  class RestMarketDataQuoteClient

  OrderController --> OrderService
  AuthController --> AccountService
  AuthController --> JwtService
  AuthController --> PasswordResetService
  AuthController --> AuthRateLimiter
  UserController --> WalletService
  UserController --> AccountService
  AdminController --> AccountService
  AdminController --> WalletService
  MarketDataController --> MarketDataService
  TradingViewController --> MarketDataService

  OrderService --> MatchingManager
  OrderService --> WalletService
  OrderService --> FeeService
  OrderService --> MarketDataService
  MatchingManager --> OrderBook
  OrderService --> MatchingEngine
  ExternalPriceOracleService --> MarketDataQuoteClient
  RestMarketDataQuoteClient ..|> MarketDataQuoteClient
  ArbitrageBotService --> ExternalPriceOracleService
  ArbitrageBotService --> OrderService
```

---

## 4. Sequence — Submit LIMIT / MARKET

```mermaid
sequenceDiagram
  actor U as User
  participant C as OrderController
  participant OS as OrderService
  participant L as SymbolLock
  participant TT as TransactionTemplate
  participant ME as MatchingEngine
  participant WS as WalletService
  participant DB as JPA / H2
  participant MM as MatchingManager
  participant MDS as MarketDataService

  U->>C: POST /api/orders/submit
  C->>OS: submitOrder(user, request)
  OS->>L: synchronized(symbol)
  OS->>TT: execute
  TT->>DB: save PENDING Order
  TT->>WS: validateAndReserve
  TT->>ME: match(book, order)
  ME-->>TT: MatchResult
  TT->>WS: settle trades + fees
  TT->>DB: update orders / trades
  TT->>TT: register afterCommit
  TT-->>OS: commit
  Note over MM,MDS: afterCommit
  OS->>MM: apply book deltas
  OS->>MDS: broadcast WS updates
  OS->>L: unlock
  OS-->>C: List of Trade
  C-->>U: 200 Order/Trade DTO
```

---

## 5. Sequence — Auth + JWT

```mermaid
sequenceDiagram
  actor C as Client
  participant AC as AuthController
  participant RL as AuthRateLimiter
  participant AS as AccountService
  participant JWT as JwtService

  C->>AC: POST /api/auth/login
  AC->>RL: check limit
  alt exceeded
    AC-->>C: 429 Too Many Requests
  else ok
    AC->>AS: authenticate
    AS-->>AC: User
    AC->>JWT: generate(userId, username, tokenVersion)
    AC-->>C: AuthResponse(token, ...)
  end
```

---

## 6. ER (спрощено)

```mermaid
erDiagram
  USER ||--|| WALLET : has
  USER ||--o{ ORDER : places
  USER ||--o{ TRANSACTION_RECORD : audited_by
  USER ||--o{ PASSWORD_RESET_TOKEN : requests
  ORDER ||--o{ TRADE : buy_or_sell
  CANDLE }o--|| MARKET : symbol

  USER {
    string id PK
    string username UK
    string email UK
    boolean admin
    int tokenVersion
    long version
  }
  WALLET {
    string ownerId PK
    long version
  }
  ORDER {
    string id PK
    string userId FK
    string baseAsset
    string quoteAsset
    string side
    string type
    string status
    string ocoGroupId
    long version
  }
  TRADE {
    string id
    string symbol
    decimal price
    decimal quantity
  }
  TRANSACTION_RECORD {
    string id PK
    string userId FK
    string asset
    decimal amount
  }
  CANDLE {
    string id PK
    string symbol
    datetime openTime
  }
```

---

## Примітки для рев’юера

1. `MatchingEngine` **не** мутує `OrderBook` напряму — лише рахує `MatchResult`.
2. Застосування дельт стакана — у `afterCommit`, щоб пам’ять = БД після успішного коміту.
3. Грошові типи `Price` / `Quantity` / `InstrumentId` — з `fin-core` (`com.alex.fin`), JPA через `AttributeConverter`.
