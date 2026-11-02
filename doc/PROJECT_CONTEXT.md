# Project Context: Crypto Exchange Simulation

## Current state

Повноцінна учбова CEX: Java 21 / Spring Boot 3 backend + React 19 frontend.

### Backend
- In-memory FIFO matching: LIMIT, MARKET, STOP_LOSS, TAKE_PROFIT, OCO
- Per-symbol locks поза TX; `TransactionTemplate` + post-commit sync стакана
- `@Version` на User / Wallet / Order; Virtual Threads
- Maker/taker fees → `SYSTEM_FEES`; wallet audit
- JWT, password reset, auth rate limit, admin API
- Domain types з `fin-core` (`Price`, `Quantity`, `InstrumentId`, `Percent`, `Money` на credit)
- Wallet storage: `Map<String, BigDecimal>` (JPA-friendly); mutations via `WalletService`
- MDS client: timeouts + Resilience4j CircuitBreaker
- Oracle + arbitrage bot; OrderBook recovery з PENDING / PARTIALLY_FILLED на старті

### Frontend
- FSD: pages / widgets / features / entities / shared
- Shared STOMP broker, RequireAuth/Admin, typed DTOs, shared UI kit
- WebSocket: book, trades, candles; TradingView Lightweight Charts + UDF

## Docs for reviewers

README → `doc/ARCHITECTURE.md` → `doc/UML.md` → `doc/REVIEW.md` → `doc/SECURITY.md`

## Pending (optional next)

- Sprint 4.1: typed wire DTO (лише з явним ТЗ — JSON контракт зараз String/BigDecimal)
- JWT httpOnly cookie (потрібні зміни backend + frontend)
- Production DB (PostgreSQL) замість H2 in-memory
