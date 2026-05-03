package com.exchange.service;

import com.exchange.engine.MatchResult;
import com.exchange.engine.MatchingEngine;
import com.exchange.engine.OrderBook;
import com.exchange.enums.OrderStatus;
import com.exchange.enums.OrderType;
import com.exchange.enums.Side;
import com.exchange.model.Order;
import com.exchange.model.Trade;
import com.exchange.model.User;
import com.exchange.model.Wallet;
import com.exchange.repository.OrderRepository;
import com.exchange.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Основной сервис биржи.
 * Координирует работу с ордерами, кошельками и стаканом заявок.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeService {
    
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    
    // Стаканы по торговым парам всё еще в памяти для скорости
    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    
    // Движок сведения ордеров
    private final MatchingEngine matchingEngine = new MatchingEngine();

    /**
     * Инициализация стаканов при старте приложения на основе активных ордеров в БД.
     */
    @PostConstruct
    public void recoverOrderBooks() {
        log.info("Starting OrderBook recovery from database...");
        List<Order> activeOrders = orderRepository.findByStatusInOrderByCreatedAtAsc(
                List.of(OrderStatus.PENDING, OrderStatus.PARTIALLY_FILLED)
        );
        
        for (Order order : activeOrders) {
            OrderBook orderBook = getOrderBook(order.getBaseAsset(), order.getQuoteAsset());
            orderBook.addOrder(order);
        }
        log.info("Recovered {} active orders into {} order books", activeOrders.size(), orderBooks.size());
    }

    /**
     * Регистрирует нового пользователя.
     */
    @Transactional
    public User registerUser(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустым");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Пароль должен быть не менее 6 символов");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }
        User user = new User(username, passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    /**
     * Получает пользователя по ID.
     */
    public User getUser(String userId) {
        return userRepository.findById(userId).orElse(null);
    }

    /**
     * Получает или создаёт стакан для торговой пары.
     */
    public OrderBook getOrderBook(String baseAsset, String quoteAsset) {
        validateAsset(baseAsset);
        validateAsset(quoteAsset);
        String symbol = baseAsset + "/" + quoteAsset;
        return orderBooks.computeIfAbsent(symbol, k -> new OrderBook(baseAsset, quoteAsset));
    }

    /**
     * Принимает ордер от пользователя.
     */
    @Transactional
    public List<Trade> submitOrder(User user, String baseAsset, String quoteAsset, 
                                   Side side, BigDecimal quantity, BigDecimal price) {
        validateSubmitOrderInputs(user, baseAsset, quoteAsset, quantity, price);
        
        user = userRepository.findById(user.getId()).orElseThrow();
        Wallet wallet = user.getWallet();
        OrderBook orderBook = getOrderBook(baseAsset, quoteAsset);
        
        // Создаём ордер
        Order order = (price != null) 
            ? Order.limitOrder(user.getId(), baseAsset, quoteAsset, side, quantity, price)
            : Order.marketOrder(user.getId(), baseAsset, quoteAsset, side, quantity);
        
        orderRepository.save(order);
        
        // Валидируем и резервируем средства
        validateAndReserve(wallet, order);
        
        // Запускаем matching engine
        MatchResult result = matchingEngine.match(orderBook, order);
        
        // Обрабатываем результаты сделок
        for (Trade trade : result.getTrades()) {
            settleTrade(trade);
        }
        
        // Синхронизируем состояние стакана в памяти ПОСЛЕ коммита в БД
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (Order toRemove : result.getOrdersToRemove()) {
                    orderBook.removeOrder(toRemove);
                }
                if (result.getRemainingOrder() != null) {
                    orderBook.addOrder(result.getRemainingOrder());
                }
            }
        });

        // Обновляем состояние резерва
        if (order.isActive()) {
            adjustReservedAmount(wallet, order);
        } else {
            releaseUnusedReserve(wallet, order, baseAsset, quoteAsset);
        }
        
        orderRepository.save(order);
        userRepository.save(user);
        
        return result.getTrades();
    }

    private void validateSubmitOrderInputs(User user, String baseAsset, String quoteAsset, BigDecimal quantity, BigDecimal price) {
        if (user == null) throw new IllegalArgumentException("Пользователь не найден");
        validateAsset(baseAsset);
        validateAsset(quoteAsset);
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Количество должно быть положительным");
        }
        if (price != null && price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Цена должна быть положительной");
        }
    }

    private void validateAsset(String asset) {
        if (asset == null || asset.isBlank()) {
            throw new IllegalArgumentException("Актив не может быть пустым");
        }
    }

    /**
     * Валидирует баланс пользователя и резервирует средства под ордер.
     */
    private void validateAndReserve(Wallet wallet, Order order) {
        if (order.getSide() == Side.BUY) {
            BigDecimal requiredAmount;
            if (order.getType() == OrderType.MARKET) {
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
        
        if (buyer == null || seller == null) {
            throw new IllegalStateException("Участник сделки не найден в системе");
        }
        
        Wallet buyerWallet = buyer.getWallet();
        Wallet sellerWallet = seller.getWallet();
        
        String baseAsset = trade.getBuyOrder().getBaseAsset();
        String quoteAsset = trade.getBuyOrder().getQuoteAsset();
        
        BigDecimal quantity = trade.getQuantity();
        BigDecimal totalAmount = trade.getTotalAmount();
        
        buyerWallet.deductReserved(quoteAsset, totalAmount);
        buyerWallet.credit(baseAsset, quantity);
        
        sellerWallet.deductReserved(baseAsset, quantity);
        sellerWallet.credit(quoteAsset, totalAmount);

        userRepository.save(buyer);
        userRepository.save(seller);
    }

    /**
     * Корректирует зарезервированную сумму после частичного исполнения ордера.
     */
    private void adjustReservedAmount(Wallet wallet, Order order) {
        BigDecimal remainingQuantity = order.getRemainingQuantity();
        
        if (order.getSide() == Side.BUY) {
            if (order.getType() == OrderType.LIMIT) {
                BigDecimal originalRequired = order.getQuantity().multiply(order.getPrice());
                BigDecimal newRequired = remainingQuantity.multiply(order.getPrice());
                BigDecimal toRelease = originalRequired.subtract(newRequired);
                if (toRelease.compareTo(BigDecimal.ZERO) > 0) {
                    wallet.unreserve(order.getQuoteAsset(), toRelease);
                }
            }
        } else {
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
     * Отменяет активный ордер.
     */
    public boolean cancelOrder(String orderId) {
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
        return "Стакан " + symbol + ": " + orderBook.getBids().size() + " price levels";
    }
}
