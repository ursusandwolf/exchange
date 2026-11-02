# Підготовка до код-рев’ю

Короткий пакет для рев’юера: що дивитись, як підняти, відомі обмеження.

## 1. Scope рев’ю

| Пріоритет | Зона | Файли / пакети |
|-----------|------|----------------|
| P0 | Matching + settlement | `engine/*`, `OrderService`, `WalletService`, `FeeService` |
| P0 | Concurrency / consistency | lock поза TX, `afterCommit`, `@Version` |
| P1 | Security | `SecurityConfig`, `Jwt*`, `PasswordReset*`, `AuthRateLimiter` |
| P1 | API contracts | `controller/*`, `dto/*`, mappers |
| P2 | Frontend FSD | `frontend/src/{widgets,features,shared}` |
| P2 | Docs vs code | цей каталог `doc/` |

## 2. Як підняти локально

1. JDK 21, Maven 3.9+, Node 20+.
2. `git clone` + `mvn install -DskipTests` для **приватного** `ursusandwolf/fin-core-lib`.
3. `mvn test` у корені `exchange`.
4. `mvn spring-boot:run` → `:8080`.
5. `cd frontend && npm i && npm run dev` → `:5173`.
6. Login admin (`ADMIN_*` env) або register звичайного юзера.

Без доступу до `fin-core-lib` збірка **не** пройде — це очікувано.

## 3. Чеклист рев’юера

- [ ] `MatchingEngine` не мутує book; дельти лише після commit
- [ ] Reserve / release / settle узгоджені для LIMIT, MARKET, cancel, OCO
- [ ] Fees йдуть на `SYSTEM_FEES`, audit пишеться
- [ ] JWT + `tokenVersion` інвалідує сесії після reset
- [ ] Admin endpoints захищені `ROLE_ADMIN`
- [ ] Rate limit на auth повертає 429
- [ ] DTO назовні, сутності не світяться в JSON напряму
- [ ] Тести покривають matching / OCO / wallet / auth controllers
- [ ] README і UML відповідають поточному коду

## 4. Відомі обмеження (не баги рев’ю)

- In-memory order books → один інстанс JVM (не кластер).
- H2 in-memory → дані зникають після рестарту процесу.
- `AuthRateLimiter` / symbol locks — процес-локальні.
- `fin-core-lib` приватний; market-data URL може бути недоступний у demo.
- Немає реального custody / blockchain / KYC.

## 5. Рекомендований порядок читання коду

1. `doc/ARCHITECTURE.md` → `doc/UML.md`  
2. `OrderService` → `MatchingEngine` → `OrderBook` → `WalletService`  
3. `SecurityConfig` + `JwtAuthenticationFilter`  
4. `MarketDataService` + frontend `widgets/*`  
5. Тести `*OrderService*` / `*MatchingEngine*` / `*WalletService*`

## 6. Гілки GitHub

| Branch | Для рев’ю |
|--------|-----------|
| **`main`** | Так — канонічний snapshot |
| `dev` | Історія ітерацій (має збігатися з main після sync) |
| `feature/start` | Ні — архів v0 прототипу |

PR / issues: орієнтуйтесь на `main`.
