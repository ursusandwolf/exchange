# Project Documentation

## Architecture Overview
The system follows a layered architecture with a clear separation between the In-Memory Matching Engine and the Persistent Database Layer.

### Service Layer (Decoupled)
1. **OrderService**: Orchestrates order placement, locking, and transaction management.
2. **AccountService**: Handles user registration and authentication.
3. **WalletService**: Manages balances, reserves, and audit logs.
4. **MatchingManager**: Maintains in-memory `OrderBook` state and handles trigger order pools.

### Matching Logic
- **FIFO**: First-In-First-Out execution for orders at the same price.
- **Trigger Activation**: `STOP_LOSS` and `TAKE_PROFIT` orders are moved to the main `OrderBook` when the last trade price hits the `triggerPrice`.

### Security
- **Concurrency**: Fine-grained locking on `String.intern()` trading pair symbols.
- **Integrity**: Post-commit synchronization ensures memory matches database state.

## Frontend (FSD)
- **Entities**: User and Auth state.
- **Features**: Place Order logic with validation.
- **Widgets**: Real-time OrderBook, Charts, and Portfolio.
- **Shared**: Base API configuration with auto-logout on 401.

## API Endpoints
- `POST /api/orders/submit`: Submit any order type.
- `POST /api/orders/{id}/cancel`: Cancel active orders.
- `GET /api/user/portfolio`: Get balances.
- `GET /api/user/history`: Get audit log.
- `GET /api/admin/stats`: Get exchange profit.
