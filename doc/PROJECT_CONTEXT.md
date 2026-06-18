# Project Context: Gemini Exchange Simulation

## Current State
A fully functional simulation of a Centralized Exchange (CEX) with a modern Java backend and React frontend.

### Backend (Java 21, Spring Boot 3.2)
- **Matching Engine**: In-memory FIFO engine. Supports LIMIT, MARKET, STOP_LOSS, and TAKE_PROFIT orders.
- **Concurrency**: Fine-grained locking per trading pair outside of database transactions. Utilizes Virtual Threads.
- **Consistency**: Post-commit memory synchronization ensuring OrderBook and Database stay in sync.
- **Audit**: Every balance change is recorded in `transaction_records`.
- **Fees**: Maker/Taker fee system with a dedicated `SYSTEM_FEES` account.
- **Integrations**: Binance Price Oracle for real BTC/USDT price data.
- **Automation**: Arbitrage bot that reacts to price deviations between internal and external markets.

### Frontend (React 19, Vite, Tailwind, TypeScript)
- **FSD Architecture**: Feature-Sliced Design.
- **Real-time**: WebSockets (STOMP) for OrderBook, Recent Trades, and Candlestick updates.
- **Charts**: Integration with TradingView Lightweight Charts.
- **Trading**: Advanced order form with validation and support for trigger prices.
- **Portfolio**: Real-time balance updates and transaction history.

### Admin
- Basic dashboard to monitor exchange profit from collected fees.

## Recent Changes
- Refactored `ExchangeService` into domain-specific services (`OrderService`, `AccountService`, `WalletService`, `MatchingManager`).
- Implemented Stop-Loss and Take-Profit logic with background activation.
- Switched to LAZY fetching for Wallet balances with safe transactional access.
- Integrated `sonner` for non-blocking UI notifications.
- Added full audit logging for all wallet operations.
- Added comprehensive unit tests for critical services (`OrderService`, `WalletService`).

## Pending Items
