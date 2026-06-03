# Migration Plan: Exchange Project Modernization

Этот план предназначен для исполнения AI-агентом. Каждый этап должен завершаться проверкой компиляции и тестами.

## Sprint 1: Domain Refactoring (fin-core-lib)
**Goal:** Transition from primitives to typed domain models from `fin-core-lib`.

- [ ] **Task 1.1: Dependency Injection**
  - Add `fin-core-lib` dependency to `exchange/pom.xml`.
  - Ensure Java 21 features are enabled.
- [ ] **Task 1.2: Order Model Upgrade**
  - Refactor `com.exchange.model.Order`:
    - `BigDecimal price` -> `com.alex.fin.core.domain.common.Price`
    - `BigDecimal quantity` -> `com.alex.fin.core.domain.common.Quantity`
    - `String baseAsset/quoteAsset` -> `com.alex.fin.core.domain.instrument.InstrumentId`
- [ ] **Task 1.3: JPA Compatibility**
  - Implement `AttributeConverter` for `Price`, `Quantity`, and `InstrumentId` to maintain database schema.
  - Apply converters to `Order`, `Trade`, and `Candle` entities.
- [ ] **Task 1.4: Validation & Logic Sync**
  - Update `Order.limitOrder` and other factory methods to use new types.
  - Update `MatchingEngine` to handle typed fields.

## Sprint 2: Financial Integrity (Wallet & Fees)
**Goal:** Secure wallet operations using shared financial primitives.

- [ ] **Task 2.1: Wallet Model Upgrade**
  - Refactor `com.exchange.model.Wallet`:
    - Change balance maps to use `CurrencyCode` as key and `Money` as value where applicable, or keep `BigDecimal` but wrap in `Money` for operations.
- [ ] **Task 2.2: WalletService Logic**
  - Use `ValidationUtils` from `fin-core-lib` for all credit/debit operations.
  - Ensure atomic updates with new domain types.
- [ ] **Task 2.3: Fee Calculation**
  - Update `FeeService` to use `com.alex.fin.core.domain.common.Percent`.
  - Refactor fee calculations to avoid precision loss.

## Sprint 3: External Integration (market-data-service)
**Goal:** Replace internal Binance oracle with a robust service-to-service integration.

- [ ] **Task 3.1: MDS Client Implementation**
  - Create a Feign or WebClient for `market-data-service`.
  - Define DTOs compatible with `fin-marketdata-api`.
- [ ] **Task 3.2: Oracle Service Refactoring**
  - Modify `ExternalPriceOracleService`:
    - Remove `RestTemplate` calls to Binance.
    - Implement fetching from `market-data-service/api/v1/quotes`.
- [ ] **Task 3.3: Arbitrage Bot Update**
  - Update `ArbitrageBotService` to consume prices from the new Oracle.
  - Ensure the bot uses `InstrumentId` for lookups.
- [ ] **Task 3.4: Resilience**
  - Add `CircuitBreaker` (Resilience4j) to the MDS client.

## Sprint 4: Finalization & Ecosystem Sync
**Goal:** Ensure full compatibility and documentation.

- [ ] **Task 4.1: API & DTO Alignment**
  - Update all Controller DTOs to ensure correct JSON serialization of `fin-core` types.
- [ ] **Task 4.2: Integration Testing**
  - Write/Update integration tests for the full flow: `MDS -> Oracle -> Bot -> MatchingEngine -> Wallet`.
- [ ] **Task 4.3: Documentation & Documentation**
  - Update `exchange/doc/ARCHITECTURE.md` and `UML.md`.
  - Update `exchange/doc/PROJECT_CONTEXT.md`.
- [ ] **Task 4.4: Final Cleanup**
  - Remove any deprecated `BigDecimal` math logic and `String` symbol constants.

---
**Definition of Done:**
1. All tasks in the sprint are checked.
2. `mvn clean compile` passes.
3. All unit and integration tests are green.
4. `docs/CHANGELOG.md` is updated.
