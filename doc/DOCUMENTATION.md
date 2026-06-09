# Project Documentation

## Architecture Overview
The system follows a layered architecture with a clear separation between the In-Memory Matching Engine and the Persistent Database Layer.

### Service Layer (Decoupled)
1. **OrderService**: Orchestrates order placement, locking, and transaction management.
2. **AccountService**: Handles user registration, admin role management, and password changes.
3. **WalletService**: Manages balances, reserves, and audit logs.
4. **MatchingManager**: Maintains in-memory `OrderBook` state and handles trigger order pools.
5. **PasswordResetService**: Issues one-time reset tokens, verifies them, and rotates JWT versions after password reset.

### Matching Logic
- **FIFO**: First-In-First-Out execution for orders at the same price.
- **Trigger Activation**: `STOP_LOSS` and `TAKE_PROFIT` orders are moved to the main `OrderBook` when the last trade price hits the `triggerPrice`.
- **OCO**: One-Cancels-the-Other pairs create two linked trigger orders, reserve base asset once up front, and cancel the sibling leg automatically when one side activates.

### Security
- **Concurrency**: Fine-grained locking on `String.intern()` trading pair symbols.
- **Integrity**: Post-commit synchronization ensures memory matches database state.
- **Auth**: JWT contains `userId`, `username`, and `tokenVersion`. Password reset increments `tokenVersion` to revoke prior sessions.
- **Authorization**: `/api/user/**` and `/api/orders/**` require `ROLE_USER` or `ROLE_ADMIN`, while `/api/admin/**` requires `ROLE_ADMIN`.
- **Rate limiting**: Auth endpoints are throttled in-memory and return `429 Too Many Requests` when limits are exceeded.

## Frontend (FSD)
- **Entities**: User and Auth state.
- **Features**: Place Order logic with validation.
- **Widgets**: Real-time OrderBook, Charts, and Portfolio.
- **Shared**: Base API configuration with auto-logout on 401.

## API Endpoints
- `POST /api/auth/register`: Register with `username`, `email`, and `password`.
- `POST /api/auth/login`: Authenticate and receive JWT.
- `POST /api/auth/password-reset/request`: Request a reset email by address.
- `POST /api/auth/password-reset/confirm`: Confirm reset with token and new password.
- `POST /api/orders/submit`: Submit any order type.
- `POST /api/orders/oco`: Submit an OCO pair of linked stop/take-profit orders.
- `POST /api/orders/{id}/cancel`: Cancel active orders.
- `GET /api/user/portfolio`: Get balances.
- `GET /api/user/history`: Get audit log.
- `GET /api/admin/stats`: Get exchange profit.
- `GET /api/admin/users`: List users for admin panel.
- `PATCH /api/admin/users/{userId}/admin`: Toggle admin role.
