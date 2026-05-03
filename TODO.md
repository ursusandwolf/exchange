# Roadmap развития Crypto Exchange Simulation

## ✅ Завершено (Phase 1 & 2)
- [x] **Security**: Интеграция Spring Security и JWT.
- [x] **Security**: Хеширование паролей (BCrypt).
- [x] **API**: Переход на DTO (Records) и Jakarta Validation.
- [x] **Reliability**: Механизм восстановления стаканов из БД при старте.
- [x] **Reliability**: Синхронизация стакана с транзакциями БД (`afterCommit`).
- [x] **Docs**: Обновление архитектурной документации.

## 🔜 В ближайших планах (Phase 3: Advanced Features)
- [ ] **Order Cancellation**:
    - [ ] Реализация метода `cancelOrder` в `ExchangeService`.
    - [ ] Возврат зарезервированных средств пользователю.
    - [ ] Синхронизация удаления из стакана в памяти.
- [ ] **User Portfolio & History**:
    - [ ] Эндпоинт получения текущих балансов.
    - [ ] Эндпоинт истории ордеров и сделок пользователя.
- [ ] **Slippage Protection**:
    - [ ] Добавление лимита проскальзывания для MARKET ордеров.

## 🚀 Будущее (Phase 4: Optimization & UX)
- [ ] **WebSockets**: Real-time трансляция стакана (OrderBook Updates).
- [ ] **Market Data Service**: Агрегация свечей (OHLCV) для графиков.
- [ ] **Java 21**: Переход на Virtual Threads для масштабируемости.
- [ ] **Fees**: Реализация системы комиссий (Maker/Taker).
