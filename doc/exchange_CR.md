Критические баги — фреймы не рендерятся
Баг №1: WebSocket обходит Vite-прокси (главная причина пустых виджетов)
В OrderBook.tsx и RecentTrades.tsx используется жёсткий URL:
typescriptconst socket = new SockJS('http://localhost:8080/ws-exchange');
// комментарий: "чтобы избежать проблем с прокси Vite"
При этом в vite.config.ts прокси настроен правильно, включая ws: true. Прямой URL вызывает CORS-ошибку: браузер отправляет запрос не через Vite (:5173), а напрямую на порт :8080. Если бэкенд не вернёт заголовок Access-Control-Allow-Origin: http://localhost:5173 — подключение упадёт, и оба виджета навсегда останутся в состоянии "Loading...".
Исправление — убрать костыль, использовать относительный URL:
typescript// было
const socket = new SockJS('http://localhost:8080/ws-exchange');
// стало
const socket = new SockJS('/ws-exchange');
Баг №2: Chart инициализируется с нулевыми размерами
В Chart.tsx вызов createChart() не передаёт width и height:
typescriptconst chart = createChart(chartContainerRef.current, {
  layout: { background: { color: 'transparent' }, ... },
  // width и height отсутствуют!
lightweight-charts читает clientWidth/clientHeight из DOM-элемента в момент вызова. В React 18 c StrictMode эффекты запускаются дважды, и при первом запуске контейнер с absolute inset-0 ещё не получил размеры от родителя. В итоге график создаётся с шириной 0 и не виден. Также в handleResize обновляется только ширина — высота не пересчитывается.
Исправление:
typescriptconst chart = createChart(chartContainerRef.current, {
  width: chartContainerRef.current.clientWidth,
  height: chartContainerRef.current.clientHeight,
  ...
});

// вместо window.addEventListener — ResizeObserver
const resizeObserver = new ResizeObserver(entries => {
  const { width, height } = entries[0].contentRect;
  chart.applyOptions({ width, height });
});
resizeObserver.observe(chartContainerRef.current);
return () => {
  resizeObserver.disconnect();
  chart.remove();
};

Нарушения архитектуры (FSD)
Нарушение направления зависимостей: shared → entities
shared/api/base.ts импортирует useAuthStore из слоя entities. По правилам FSD слой shared не может знать ни о каком слое выше себя. Это нарушение создаёт скрытую циклическую зависимость и делает shared непереиспользуемым.
typescript// shared/api/base.ts — НАРУШЕНИЕ
import { useAuthStore } from '../../entities/user/model/authStore';
Парадокс в том, что сам интерсептор уже правильно читает токен из localStorage напрямую — импорт useAuthStore нужен только для вызова logout() при 401. Правильное решение — инжектировать callback через фабрику:
typescript// shared/api/base.ts
export const createApiClient = (onUnauthorized?: () => void) => {
  const instance = axios.create({ baseURL: '/api' });
  instance.interceptors.response.use(
    r => r,
    error => {
      if (error.response?.status === 401) onUnauthorized?.();
      return Promise.reject(error);
    }
  );
  return instance;
};

// app/App.tsx — подключение
const api = createApiClient(() => useAuthStore.getState().logout());
Отсутствует слой features/
Вся логика торговли (размещение ордеров, мутация, валидация, обработка ошибок) находится в widgets/TradingForm.tsx. По FSD это должен быть отдельный feature-слайс features/place-order/, а виджет — лишь компоновать его в UI.
Нет Public API (barrel exports) у слайсов
Все импорты лезут напрямую в глубину: ../entities/user/model/authStore. Каждый слайс должен иметь index.ts, который контролирует, что именно он экспортирует наружу.

Двойной App.tsx — мусорный файл
В корне src/App.tsx лежит дефолтный Vite scaffold (с логотипами Vite и React, счётчиком кликов). Настоящее приложение находится в src/app/App.tsx. main.tsx правильно импортирует из ./app/App, поэтому scaffold никогда не используется — но он создаёт путаницу и должен быть удалён.

Дублирование токена в authStore
typescriptsetAuth: (token, username, userId) => {
  localStorage.setItem('token', token);   // ← ключ 'token'
  set({ token, username, userId });       // ← persist пишет в 'auth-storage'
},
Токен хранится в двух местах под разными ключами. api/base.ts читает из 'token', persist восстанавливает из 'auth-storage'. После перезагрузки страницы persist корректно восстановит Zustand-стейт, но если вручную очистить localStorage, состояния разойдутся. Нужно либо полностью довериться persist (убрать ручной localStorage.setItem), либо отказаться от persist и управлять хранилищем вручную — но не оба подхода одновременно.

tailwind.config.js несовместим с Tailwind v4
В проекте используется Tailwind CSS v4 (^4.2.4). В v4 конфигурация производится через CSS (@theme { ... } в index.css — что уже сделано правильно). Файл tailwind.config.js в v4 больше не читается движком. Его наличие вводит в заблуждение — кажется, что там можно что-то настроить, но это не так.

Мелкие замечания
alert() в TradingForm для обратной связи пользователю — нужен toast. Мутация принимает (newOrder: any) вместо типизированного интерфейса. В RecentTrades нет начальной загрузки исторических данных через HTTP — компонент начинает с пустого состояния и ждёт только WebSocket-событий; если подключение упало, история не покажется никогда.

Итоговый приоритет правок

Заменить абсолютный WS-URL на относительный /ws-exchange — это разблокирует OrderBook и RecentTrades.
Передать width/height в createChart и заменить window.resize на ResizeObserver — это исправит рендер графика.
Убрать импорт useAuthStore из shared/api/base.ts, инжектировать callback.
Удалить src/App.tsx (Vite scaffold).
Разрешить дублирование токена — выбрать один механизм.
Добавить слой features/, вынести торговую логику из TradingForm.
Добавить index.ts к каждому слайсу.
Удалить tailwind.config.js.