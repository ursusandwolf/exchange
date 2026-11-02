# Project Context: Crypto Exchange Simulation

## Current state

Повноцінна учбова CEX: Java 21 / Spring Boot 3 backend + React 19 frontend.

### Backend
- In-memory FIFO matching: LIMIT, MARKET, STOP_LOSS, TAKE_PROFIT, OCO
- Per-symbol locks поза TX; `TransactionTemplate` + post-commit sync стакана
- `@Version` на User / Wallet / Order; Virtual Threads
- Maker/taker fees → `SYSTEM_FEES`; wallet audit
- JWT, password reset, auth rate limit, admin API
- Domain types з `fin-core` (`Price`, `Quantity`, `InstrumentId`)
- Oracle + arbitrage bot (через market-data client)

### Frontend
- FSD: pages / widgets / features / entities / shared
- WebSocket (STOMP): book, trades, candles
- TradingView Lightweight Charts + UDF backend

## Docs for reviewers

README → `doc/ARCHITECTURE.md` → `doc/UML.md` → `doc/REVIEW.md` → `doc/SECURITY.md`

## Pending (optional next)

- Circuit breaker на MDS client
- Wallet balances → typed `Money` / `CurrencyCode` (Sprint 2 у `MIGRATION_PLAN.md`)
- Production DB (PostgreSQL) замість H2 in-memory
