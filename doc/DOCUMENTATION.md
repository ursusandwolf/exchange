# Project Documentation

## Backend services

| Service | Responsibility |
|---------|----------------|
| `OrderService` | Place / cancel / OCO; per-symbol lock; TX + matching |
| `MatchingManager` | In-memory books + trigger pool; DB restore |
| `WalletService` | Balances, reserves, settlement, audit rows |
| `AccountService` | Users, admin flag, password change |
| `FeeService` | Maker / taker fee rates |
| `MarketDataService` | Candles (OHLCV) + WebSocket fan-out |
| `JwtService` | Issue / parse JWT |
| `PasswordResetService` | Token lifecycle + mailer + `tokenVersion` bump |
| `AuthRateLimiter` | Sliding-window limits on auth endpoints |
| `ExternalPriceOracleService` | Quotes via `MarketDataQuoteClient` |
| `ArbitrageBotService` | Demo trading on oracle vs book gap |

## Matching rules

- Same price level → **FIFO**.
- `STOP_LOSS` / `TAKE_PROFIT` stay outside the book until `triggerPrice` is hit by last trade.
- **OCO**: two linked triggers; one base reserve; sibling cancelled on activation.
- Engine returns **`MatchResult`** only; book mutation after successful commit.

## Security (summary)

- JWT claims: `userId`, `username`, `tokenVersion`.
- Password reset increments `tokenVersion` → old tokens invalid.
- `/api/user/**`, `/api/orders/**` → `ROLE_USER` or `ROLE_ADMIN`.
- `/api/admin/**` → `ROLE_ADMIN`.
- Public: auth, selected market/chart endpoints.
- Details: [SECURITY.md](SECURITY.md).

## REST API

| Method | Path | Auth | Note |
|--------|------|------|------|
| POST | `/api/auth/register` | — | username, email, password |
| POST | `/api/auth/login` | — | JWT |
| POST | `/api/auth/password-reset/request` | — | by email |
| POST | `/api/auth/password-reset/confirm` | — | token + newPassword |
| POST | `/api/orders/submit` | user | any order type |
| POST | `/api/orders/oco` | user | linked stop/TP |
| POST | `/api/orders/{id}/cancel` | user | release reserve |
| GET | `/api/user/portfolio` | user | balances |
| GET | `/api/user/history` | user | audit |
| GET | `/api/admin/stats` | admin | fees / stats |
| GET | `/api/admin/users` | admin | users + balances |
| PATCH | `/api/admin/users/{id}/admin` | admin | toggle role |
| GET | market / TradingView UDF | public* | charts & book snapshots |

\*Точний whitelist — у `SecurityConfig`.

## Frontend (FSD)

- **entities/user** — auth Zustand store  
- **features/place-order** — форма + Zod  
- **widgets** — OrderBook, Chart, Portfolio, RecentTrades, TradingForm  
- **shared/api** — base client, React Query, auto-logout on 401  

## Run & test

```bash
# backend
mvn spring-boot:run
mvn test

# frontend
cd frontend && npm install && npm run dev
npm run lint
```

Потрібен локальний `mvn install` з [fin-core-lib](https://github.com/ursusandwolf/fin-core-lib).

## Review package

Див. [REVIEW.md](REVIEW.md) і [UML.md](UML.md).
