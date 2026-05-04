# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Fixed
- **WebSocket:** Changed absolute URL `http://localhost:8080/ws-exchange` to relative `/ws-exchange` in `OrderBook` and `RecentTrades` to fix Vite proxying and CORS issues.
- **Chart:** Fixed initialization with zero dimensions by passing container clientWidth/clientHeight to `createChart`.
- **Chart:** Replaced `window.resize` event listener with `ResizeObserver` for robust responsiveness.
- **Auth:** Resolved token duplication by relying solely on Zustand `persist` middleware; removed manual `localStorage` calls.
- **Lint:** Fixed various TypeScript lint errors (removed `any` types, unused variables).

### Changed
- **Architecture:** Refactored to follow Feature-Sliced Design (FSD) more strictly.
- **Architecture:** Extracted order placement logic from `widgets/TradingForm` to `features/place-order`.
- **API:** Refactored `shared/api` to remove circular dependency on `entities/user`. Now uses an injection pattern for the logout callback.
- **Imports:** Established Public APIs (barrel exports) for all FSD layers (`entities`, `features`, `widgets`, `shared/api`).

### Removed
- **Scaffold:** Deleted redundant `src/App.tsx` (Vite default scaffold).
- **Config:** Removed `tailwind.config.js` (unnecessary in Tailwind CSS v4).
