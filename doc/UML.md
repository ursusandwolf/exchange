# UML Diagrams

## Service Dependencies (Decoupled Design)

```mermaid
[Controllers]
      |
      v
[OrderService] ----> [MatchingManager]
      |                   |
      |                   v
      +------------> [OrderBook]
      |                   ^
      v                   |
[WalletService] <--- [MatchingEngine]
      |
      v
[AuditRepository]
```

## Order Execution Flow

```mermaid
User -> OrderController: Submit Request
OrderController -> OrderService: submitOrder()
OrderService -> Locks: Lock(Symbol)
OrderService -> TransactionTemplate: Start Transaction
TransactionTemplate -> OrderRepository: Save PENDING
TransactionTemplate -> MatchingEngine: match()
MatchingEngine -> MatchResult: Trades + Deltas
TransactionTemplate -> WalletService: Settle (Buyer, Seller, Fees)
TransactionTemplate -> OrderRepository: Update Filled/Status
TransactionTemplate -> PostCommit: Register Sync
TransactionTemplate -> COMMIT
PostCommit -> OrderBook: Apply Changes
PostCommit -> WebSockets: Broadcast
OrderService -> Locks: Unlock(Symbol)
```
