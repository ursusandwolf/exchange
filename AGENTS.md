# AI Agent Map — exchange

Читай спочатку: `ARCHITECT_CODE_REVIEW.txt`, `doc/ARCHITECTURE.md`, `doc/REVIEW.md`.

## Що це
Учбова CEX: Java 21 / Spring Boot 3 matching + React 19 FSD UI.

## Інваріанти (не ламати)
1. `MatchingEngine` лише рахує `MatchResult` — не мутує `OrderBook`.
2. Баланси тільки через `WalletService` (audit).
3. Lock по символу **поза** `TransactionTemplate`; sync стакана в `afterCommit`.
4. API назовні через DTO/mapper, не Entity.

## Куди класти код
| Зміна | Пакет |
|-------|--------|
| Новий REST | `controller` + `dto` |
| Бізнес-сценарій | `service` |
| Алгоритм match | `engine` |
| Persist | `model` + `repository` |
| Зовнішній HTTP | `client` + interface |
| Security/WS/JPA conv | `config` |

## Гарячі файли
`OrderService`, `MatchingEngine`, `OrderBook`, `Wallet`, `MatchingManager`, `SecurityConfig`

## Build
```
# потрібен локальний fin-core-lib (com.alex.fin:fin-core:0.1.0)
mvn test
mvn spring-boot:run
cd frontend && npm i && npm run dev
```

## Стан спринтів
Phase 1–6 + Sprint 1 (fin-core types) ≈ done.  
Далі: Sprint 2 (Money types) / Sprint 3 (MDS resilience) / fix partial-fill reserve.

## Не робити без явного ТЗ
Мікросервіси, Kafka, rewrite frontend, зміна публічних API контрактів, force-push.
