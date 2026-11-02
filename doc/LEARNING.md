# Навчальний гід по коду Exchange

Повний маршрут і глосарій також у `ARCHITECT_CODE_REVIEW.txt` §8.

## 7 кроків

1. **Wallet** — `model/Wallet.java`, `service/WalletService.java`  
   Reserve ≠ debit. Audit row на кожну мутацію.
2. **OrderBook** — bids ↓ / asks ↑, FIFO на рівні ціни.
3. **MatchingEngine** — чистий розрахунок → `MatchResult`. Без запису в book.
4. **OrderService.executeOrderTransaction** — save → reserve → match → settle → afterCommit.
5. **Locks** — `synchronized(symbol)` зовні TX.
6. **JWT** — claims + `tokenVersion` інвалідація після reset.
7. **Frontend widgets** — дзеркало book/trades через STOMP.

## Вправа
Постав LIMIT SELL 1 BTC @ 45000, LIMIT BUY 1 BTC @ 45000.  
Простеж у дебагері: `validateAndReserve` → `match` → `settleTrade` → WS broadcast.
