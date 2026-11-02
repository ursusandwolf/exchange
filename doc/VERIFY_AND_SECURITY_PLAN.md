# Plan: Verification + Security Audit

Дата: 2026-11-02  
Мета: підтвердити зелені тести/збірку і зафіксувати security-ризики для учбової CEX.

## Phase A — Functional verify
1. `mvn test` (backend)
2. `cd frontend && npm test && npm run build`
3. Ручний smoke (за наявності запущених сервісів): login → trade → reserve/cancel → admin 403

## Phase B — Security audit
1. Config: `SecurityConfig`, JWT secret/env, CSRF/CORS, public vs protected routes
2. Auth: BCrypt, rate limit, password reset enumeration, `tokenVersion`
3. AuthZ: admin endpoints, IDOR (portfolio/orders by principal)
4. Money path: баланси лише `WalletService`, no entity leak у JSON
5. Frontend: JWT storage, XSS surface, 401 logout
6. Підсумок у `doc/SECURITY_AUDIT.txt` (P0/P1/P2 + вердикт)

## Phase C — Follow-up (лише з ТЗ)
- httpOnly cookies, refresh tokens, PostgreSQL, distributed rate-limit
