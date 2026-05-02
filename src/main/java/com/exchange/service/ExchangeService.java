package com.exchange.service;

import com.exchange.engine.MatchingEngine;
import com.exchange.engine.OrderBook;
import com.exchange.enums.Side;
import com.exchange.model.Order;
import com.exchange.model.Trade;
import com.exchange.model.User;
import com.exchange.model.Wallet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Основной сервис биржи.
 * Координирует работу с ордерами, кошельками и стаканом заявок.
 */
public class ExchangeService {
    
    // Хранилище пользователей: userId -> User
    private final Map<String, User> users;
    
    // Стаканы по торговым парам: symbol -> OrderBook
    private final Map<String, OrderBook> orderBooks;
    
    // Движок сведения ордеров
    private final MatchingEngine matchingEngine;

    public ExchangeService() {
        this.users = new ConcurrentHashMap<>();
        this.orderBooks = new ConcurrentHashMap<>();
        this.matchingEngine = new MatchingEngine();
    }

    /**
     * Регистрирует нового пользователя.
     */
    public User registerUser(String username) {
        User user = new User(username);
        users.put(user.getId(), user);
        return user;
    }

    /**
     * Получает пользователя по ID.
     */
    public User getUser(String userId) {
        return users.get(userId);
    }

    /**
     * Получает или создаёт стакан для торговой пары.
     */
    public OrderBook getOrderBook(String baseAsset, String quoteAsset) {
        String symbol = baseAsset + "/" + quoteAsset;
        return orderBooks.computeIfAbsent(symbol, k -> new OrderBook(baseAsset, quoteAsset));
    }

    /**
     * Принимает ордер от пользователя.
     * 
     * @param user Пользователь, выставляющий ордер
     * @param baseAsset Базовый актив (например, BTC)
     * @param quoteAsset Котируемый актив (например, USDT)
     * @param side Направление ордера (BUY/SELL)
     * @param quantity Количество базового актива
     * @param price Цена за единицу (null для MARKET ордеров)
     * @return Список совершённых сделок
     * 
     * // TODO: Здесь можно добавить:
     * // - Валидацию минимального/максимального размера ордера
     * // - Проверку лимитов риска (exposure limits)
     * // - Аудит действий пользователя
     * // - Интеграцию с системой уведомлений (WebSocket, Kafka)
     * // - Persistency: сохранение ордера в БД перед исполнением
     */
    public List<Trade> submitOrder(User user, String baseAsset, String quoteAsset, 
                                   Side side, BigDecimal quantity, BigDecimal price) {
        Wallet wallet = user.getWallet();
        OrderBook orderBook = getOrderBook(baseAsset, quoteAsset);
        
        // Создаём ордер
        Order order = (price != null) 
            ? Order.limitOrder(user.getId(), baseAsset, quoteAsset, side, quantity, price)
            : Order.marketOrder(user.getId(), baseAsset, quoteAsset, side, quantity);
        
        // Валидируем и резервируем средства
        validateAndReserve(wallet, order);
        
        // Запускаем matching engine
        List<Trade> trades = matchingEngine.match(orderBook, order);
        
        // Обрабатываем результаты сделок: переводим средства между кошельками
        for (Trade trade : trades) {
            settleTrade(trade);
        }
        
        // Если ордер не полностью исполнен и остался в стакане — 
        // пересчитываем зарезервированные средства
        if (order.isActive()) {
            adjustReservedAmount(wallet, order);
        } else {
            // Ордер полностью исполнен или отменён — освобождаем остаток резерва
            releaseUnusedReserve(wallet, order, baseAsset, quoteAsset);
        }
        
        return trades;
    }

    /**
     * Валидирует баланс пользователя и резервирует средства под ордер.
     */
    private void validateAndReserve(Wallet wallet, Order order) {
        if (order.getSide() == Side.BUY) {
            // Для BUY ордера резервируем котируемый актив (USDT)
            BigDecimal requiredAmount;
            if (order.getType().toString().equals("MARKET")) {
                // Для MARKET ордера резервируем сумму по текущей лучшей цене ask
                // В реальном проекте нужно получать цену из стакана
                OrderBook orderBook = getOrderBookByAssets(order.getBaseAsset(), order.getQuoteAsset());
                BigDecimal bestAsk = orderBook != null ? orderBook.getBestAsk() : null;
                if (bestAsk != null) {
                    requiredAmount = order.getQuantity().multiply(bestAsk);
                } else {
                    throw new IllegalArgumentException("Нет доступных ордеров на продажу для MARKET ордера");
                }
            } else {
                requiredAmount = order.getQuantity().multiply(order.getPrice());
            }
            wallet.reserve(order.getQuoteAsset(), requiredAmount);
        } else {
            // Для SELL ордера резервируем базовый актив (BTC)
            wallet.reserve(order.getBaseAsset(), order.getQuantity());
        }
    }

    /**
     * Вспомогательный метод для получения стакана по активам.
     */
    private OrderBook getOrderBookByAssets(String baseAsset, String quoteAsset) {
        String symbol = baseAsset + "/" + quoteAsset;
        return orderBooks.get(symbol);
    }

    /**
     * Исполняет сделку: переводит активы между покупателями и продавцами.
     */
    private void settleTrade(Trade trade) {
        User buyer = getUser(trade.getBuyerId());
        User seller = getUser(trade.getSellerId());
        
        Wallet buyerWallet = buyer.getWallet();
        Wallet sellerWallet = seller.getWallet();
        
        String baseAsset = trade.getBuyOrder().getBaseAsset();
        String quoteAsset = trade.getBuyOrder().getQuoteAsset();
        
        BigDecimal quantity = trade.getQuantity();
        BigDecimal totalAmount = trade.getTotalAmount();
        
        // Покупатель получает базовый актив, продавец получает котируемый
        // Списываем зарезервированные средства
        
        // Buyer: списываем зарезервированный quote asset, зачисляем base asset
        buyerWallet.deductReserved(quoteAsset, totalAmount);
        buyerWallet.credit(baseAsset, quantity);
        
        // Seller: списываем зарезервированный base asset, зачисляем quote asset
        sellerWallet.deductReserved(baseAsset, quantity);
        sellerWallet.credit(quoteAsset, totalAmount);
        
        // TODO: Здесь можно добавить:
        // - Удержание комиссий перед зачислением
        // - Начисление комиссий бирже
        // - Отправку уведомлений участникам сделки
        // - Логирование в audit trail
    }

    /**
     * Корректирует зарезервированную сумму после частичного исполнения ордера.
     */
    private void adjustReservedAmount(Wallet wallet, Order order) {
        BigDecimal remainingQuantity = order.getRemainingQuantity();
        
        if (order.getSide() == Side.BUY) {
            // Для BUY: пересчитываем резерв по оставшемуся количеству
            BigDecimal originalRequired = getOriginalRequiredAmount(order);
            BigDecimal newRequired = remainingQuantity.multiply(order.getPrice());
            BigDecimal toRelease = originalRequired.subtract(newRequired);
            if (toRelease.compareTo(BigDecimal.ZERO) > 0) {
                wallet.unreserve(order.getQuoteAsset(), toRelease);
            }
        } else {
            // Для SELL: освобождаем разницу между исходным и оставшимся количеством
            BigDecimal toRelease = order.getQuantity().subtract(remainingQuantity);
            if (toRelease.compareTo(BigDecimal.ZERO) > 0) {
                wallet.unreserve(order.getBaseAsset(), toRelease);
            }
        }
    }

    /**
     * Освобождает неиспользованные зарезервированные средства после полного исполнения ордера.
     */
    private void releaseUnusedReserve(Wallet wallet, Order order, String baseAsset, String quoteAsset) {
        if (order.getSide() == Side.BUY) {
            BigDecimal reserved = wallet.getReserved(quoteAsset);
            if (reserved.compareTo(BigDecimal.ZERO) > 0) {
                wallet.unreserve(quoteAsset, reserved);
            }
        } else {
            BigDecimal reserved = wallet.getReserved(baseAsset);
            if (reserved.compareTo(BigDecimal.ZERO) > 0) {
                wallet.unreserve(baseAsset, reserved);
            }
        }
    }

    /**
     * Вычисляет исходную зарезервированную сумму для ордера.
     */
    private BigDecimal getOriginalRequiredAmount(Order order) {
        if (order.getSide() == Side.BUY) {
            return order.getQuantity().multiply(order.getPrice());
        } else {
            return order.getQuantity();
        }
    }

    /**
     * Отменяет активный ордер.
     * Возвращает true, если ордер был найден и отменён.
     * 
     * // TODO: Добавить persistency: запись события отмены в БД
     */
    public boolean cancelOrder(String orderId) {
        // В упрощённой версии не отслеживаем все ордеры глобально
        // В реальном проекте нужен реестр всех активных ордеров
        throw new UnsupportedOperationException("Отмена ордера требует глобального реестра ордеров");
    }

    /**
     * Возвращает список всех торговых пар.
     */
    public List<String> getSymbols() {
        return new ArrayList<>(orderBooks.keySet());
    }

    /**
     * Получает статистику по стакану.
     */
    public String getOrderBookStats(String symbol) {
        OrderBook orderBook = orderBooks.get(symbol);
        if (orderBook == null) {
            return "Стакан не найден: " + symbol;
        }
        return "Стакан " + symbol + ": " + orderBook.getBidCount() + " bids, " 
               + orderBook.getAskCount() + " asks";
    }
}
