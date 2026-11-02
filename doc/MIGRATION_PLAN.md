# Migration Plan: Exchange Project Modernization

Этот план предназначен для исполнения AI-агентом. Каждый этап должен завершаться проверкой компиляции и тестами.

## Sprint 1: Domain Refactoring (fin-core-lib)
**Goal:** Transition from primitives to typed domain models from `fin-core-lib`.

- [x] **Task 1.1: Dependency Injection**
  - Add `fin-core` dependency to `exchange/pom.xml`.
  - Ensure Java 21 features are enabled.
- [x] **Task 1.2: Order Model Upgrade**
  - `Price` / `Quantity` / `InstrumentId` on `Order` (and related engine paths).
- [x] **Task 1.3: JPA Compatibility**
  - `PriceConverter`, `QuantityConverter`, `InstrumentIdConverter` (`autoApply`).
- [x] **Task 1.4: Validation & Logic Sync**
  - Factories `limitOrder` / `marketOrder` / `triggerOrder` + MatchingEngine typed fields.

## Sprint 2: Financial Integrity (Wallet & Fees)
**Goal:** Secure wallet operations using shared financial primitives.

- [x] **Task 2.1: Wallet Model Upgrade**
  - Storage remains `Map<String, BigDecimal>` (JPA-friendly); added `credit(Money)` and `ValidationUtils` on mutations.
- [x] **Task 2.2: WalletService Logic**
  - Asset names normalized via `ValidationUtils`; single `mutate` path with audit.
- [x] **Task 2.3: Fee Calculation**
  - `FeeService` uses `Percent` + scale HALF_UP.

## Sprint 3: External Integration (market-data-service)
**Goal:** Replace internal Binance oracle with a robust service-to-service integration.

- [x] **Task 3.1: MDS Client Implementation**
  - `MarketDataQuoteClient` + `RestMarketDataQuoteClient`.
- [x] **Task 3.2: Oracle Service Refactoring**
  - `ExternalPriceOracleService` fetches via client (no Binance).
- [x] **Task 3.3: Arbitrage Bot Update**
  - Bot uses `InstrumentId` for base/quote; oracle prices.
- [x] **Task 3.4: Resilience**
  - Connect/read timeouts + Resilience4j `CircuitBreaker` on quote client.

## Sprint 4: Finalization & Ecosystem Sync
**Goal:** Ensure full compatibility and documentation.

- [ ] **Task 4.1: API & DTO Alignment**
  - Controllers still expose `String`/`BigDecimal` DTO (intentional JSON stability).
- [x] **Task 4.2: Integration Testing**
  - Order / OCO / wallet / fees / partial-fill reserve tests green.
- [x] **Task 4.3: Documentation**
  - Architecture, UML, REVIEW, ARCHITECT_CODE_REVIEW, LEARNING, AGENTS.
- [~] **Task 4.4: Final Cleanup**
  - Hardening: `adjustReservedAmount`, memory sync only in `afterCommit`.
  - Remaining: full Money maps in JPA if needed later.

---
**Definition of Done:**
1. Sprint tasks marked above.
2. `mvn test` passes.
3. Docs reflect current state.
