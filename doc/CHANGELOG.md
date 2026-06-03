# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased] - 2026-06-03

### Added
- **Core Domain**: Decoupled `ExchangeService` into `AccountService`, `WalletService`, `OrderService`, and `MatchingManager`.
- **Advanced Orders**: Support for `STOP_LOSS` and `TAKE_PROFIT` orders with background trigger activation.
- **Audit System**: Full transaction history for wallets in `transaction_records`.
- **Admin**: `AdminController` to monitor exchange fee revenue.
- **Frontend**: `sonner` for modern notifications.
- **Frontend**: Real-time candlestick updates via WebSockets in `Chart` widget.
- **Frontend**: Multi-tab `Portfolio` widget (Assets + History).
- **Automation**: `ArbitrageBotService` to sync internal price with Binance Oracle.
- **Observability**: `doc/PROJECT_CONTEXT.md` for project state synchronization.

### Fixed
- **Consistency**: Resolved "Match-before-Commit" race condition by using `TransactionTemplate` and post-commit synchronization.
- **Concurrency**: Moved synchronization locks outside of database transactions to prevent lost updates and double spending.
- **Performance**: Switched `Wallet` balances to `LAZY` loading with safe transactional getters.
- **Compilation**: Fixed missing `BigDecimal` imports in `MatchingManager`.
- **UX**: Replaced blocking `alert()` calls with non-blocking toasts.

### Removed
- `ExchangeService.java` (God Class).
- `ExchangeServiceIntegrationTest.java` (replaced by `OrderServiceIntegrationTest`).
