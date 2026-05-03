# Roadmap развития Crypto Exchange Simulation

## ✅ КРИТИЧЕСКИЕ ИСПРАВЛЕНИЯ (Security & Stability)
- [x] **Concurrency**: Исправлен Race Condition в `submitOrder` (блокировка по паре).
- [x] **Consistency**: Добавлен `@Version` (Optimistic Locking) в `Order`, `Wallet`, `User`.
- [x] **Architecture**: Маппинг сущностей в DTO перед возвратом из API (TradeResponse).

## ✅ Завершено (Phase 3: Advanced Features)
- [x] **Order Cancellation**:
    - [x] Реализован метод `cancelOrder` в `ExchangeService`.
    - [x] Возврат зарезервированных средств пользователю.
    - [x] Синхронизация удаления из стакана в памяти.
- [x] **User Portfolio & History**:
    - [x] Эндпоинт получения текущих балансов (`/api/user/portfolio`).
    - [x] Эндпоинт истории ордеров пользователя (`/api/orders/history`).
- [x] **Slippage Protection**:
    - [x] Добавлен лимит проскальзывания `priceLimit` для MARKET ордеров.

## ✅ Завершено (Phase 1 & 2)
- [x] **Security**: Интеграция Spring Security и JWT.
- [x] **Security**: Хеширование паролей (BCrypt).
- [x] **API**: Переход на DTO (Records) и Jakarta Validation.
- [x] **Reliability**: Механизм восстановления стаканов из БД при старте.
- [x] **Reliability**: Синхронизация стакана с транзакциями БД (`afterCommit`).
- [x] **Docs**: Обновление архитектурной документации.

## ✅ Завершено (Phase 4: Optimization & UX)
- [x] **WebSockets**: Real-time трансляция стакана (OrderBook Updates).
- [x] **Market Data Service**: Агрегация свечей (OHLCV) для графиков.
- [x] **Java 21**: Переход на Virtual Threads для масштабируемости.
- [x] **Fees**: Реализация системы комиссий (Maker/Taker).

## ✅ Завершено (Phase 6: Frontend)
- [x] **Setup**: Инициализация React + Vite + TypeScript.
- [x] **Auth**: Страницы Login/Register с сохранением JWT.
- [x] **UX**: Улучшенная обработка ошибок (400 Bad Request) на страницах входа.
- [x] **Dashboard**: Главная страница с Layout.
- [x] **Real-time**: Виджеты OrderBook и Recent Trades через WebSockets (STOMP).
- [ ] **Market**: Компонент графика TradingView (Lightweight Charts).
- [ ] **Trading**: Форма создания лимитных и рыночных ордеров.
- [ ] **Portfolio**: Отображение балансов и истории ордеров пользователя.

## ✅ СТАБИЛЬНОСТЬ И ОШИБКИ (Recent Fixes)
- [x] **Startup Fix**: Исправлена ошибка инициализации демо-данных (Insufficient funds) через персистентный метод `deposit`.
- [x] **Test Isolation**: Изоляция `MarketDataInitializer` через профили (`!test`) для предотвращения загрязнения интеграционных тестов.

## 🚀 Будущее (Phase 5: External Integrations)
- [x] **Price Oracle**: Получение реальных цен с Binance API (`ExternalPriceOracleService`).
- [x] **TradingView**: Интеграция легковесного чарта для отображения свечей (UDF API).
- [ ] **Arbitrage Bots**: Демонстрационные боты, торгующие на основе разницы цен Oracle и внутренней биржи.

