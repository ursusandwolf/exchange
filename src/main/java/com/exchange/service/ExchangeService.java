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
import jakarta.persistence.EntityManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeService {
    
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final MarketDataService marketDataService;
    private final FeeService feeService;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    // Стаканы по торговым парам всё еще в памяти для скорости
    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();

    // Движок сведения ордеров
    private MatchingEngine matchingEngine;

    // Блокировки по торговым парам
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    /**
     * Инициализация стаканов при старте приложения на основе активных ордеров в БД.
     */
    @PostConstruct
    public void init() {
        this.matchingEngine = new MatchingEngine(feeService);
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
     * Использует блокировку по паре для обеспечения атомарности матчинга.
     */
    public List<Trade> submitOrder(User user, String baseAsset, String quoteAsset, 
                                   Side side, BigDecimal quantity, BigDecimal price, BigDecimal priceLimit) {
        String symbol = baseAsset + "/" + quoteAsset;
        Object lock = locks.computeIfAbsent(symbol, k -> new Object());
        
        synchronized (lock) {
            return executeOrderTransaction(user, baseAsset, quoteAsset, side, quantity, price, priceLimit);
        }
    }

    @Transactional
    protected List<Trade> executeOrderTransaction(User user, String baseAsset, String quoteAsset, 
                                                Side side, BigDecimal quantity, BigDecimal price, BigDecimal priceLimit) {
        validateSubmitOrderInputs(user, baseAsset, quoteAsset, quantity, price);
        
        // Перечитываем пользователя через EntityManager, чтобы гарантировать свежее состояние (для тестов и конкурентности)
        user = entityManager.find(User.class, user.getId());
        if (user == null) throw new IllegalArgumentException("Пользователь не найден");
        
        Wallet wallet = user.getWallet();
        OrderBook orderBook = getOrderBook(baseAsset, quoteAsset);
        
        // Создаём ордер
        Order order = (price != null) 
            ? Order.limitOrder(user.getId(), baseAsset, quoteAsset, side, quantity, price)
            : Order.marketOrder(user.getId(), baseAsset, quoteAsset, side, quantity, priceLimit);
        
        orderRepository.save(order);
        
        // Валидируем и резервируем средства
        validateAndReserve(wallet, order);
        
        // Сбрасываем изменения в БД, чтобы гарантировать видимость зарезервированных средств
        entityManager.flush();
        
        // Запускаем matching engine
        MatchResult result = matchingEngine.match(orderBook, order);
        
        // Обрабатываем результаты сделок
        for (Trade trade : result.getTrades()) {
            settleTrade(trade);
        }
        
        // Синхронизируем состояние стакана в памяти ПОСЛЕ коммита в БД
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (Order toRemove : result.getOrdersToRemove()) {
                        orderBook.removeOrder(toRemove);
                    }
                    if (result.getRemainingOrder() != null) {
                        orderBook.addOrder(result.getRemainingOrder());
                    }
                    
                    // Broadcast updates
                    result.getTrades().forEach(marketDataService::broadcastTrade);
                    marketDataService.broadcastOrderBookUpdate(orderBook);
                }
            });
        } else {
            // Fallback for non-transactional calls (e.g., some tests)
            for (Order toRemove : result.getOrdersToRemove()) {
                orderBook.removeOrder(toRemove);
            }
            if (result.getRemainingOrder() != null) {
                orderBook.addOrder(result.getRemainingOrder());
            }
            result.getTrades().forEach(marketDataService::broadcastTrade);
            marketDataService.broadcastOrderBookUpdate(orderBook);
        }

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
            // Резервируем с учетом комиссии Taker (на всякий случай, как максимум)
            BigDecimal estimatedFee = feeService.calculateTakerFee(requiredAmount);
            BigDecimal toReserve = requiredAmount.add(estimatedFee);
            log.info("Reserving {} USDT for user {} (including estimated fee {})", toReserve, wallet.getOwnerId(), estimatedFee);
            wallet.reserve(order.getQuoteAsset(), toReserve);
        } else {
            log.info("Reserving {} {} for user {}", order.getQuantity(), order.getBaseAsset(), wallet.getOwnerId());
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
        // Перечитываем участников сделки, чтобы иметь актуальные кошельки с зарезервированными средствами
        User buyer = entityManager.find(User.class, trade.getBuyerId());
        User seller = entityManager.find(User.class, trade.getSellerId());
        
        if (buyer == null || seller == null) {
            throw new IllegalStateException("Участник сделки не найден в системе");
        }
        
        Wallet buyerWallet = buyer.getWallet();
        Wallet sellerWallet = seller.getWallet();
        
        String baseAsset = trade.getBuyOrder().getBaseAsset();
        String quoteAsset = trade.getBuyOrder().getQuoteAsset();
        
        BigDecimal quantity = trade.getQuantity();
        BigDecimal totalAmount = trade.getTotalAmount();
        BigDecimal buyerFee = trade.getBuyerFee();
        BigDecimal sellerFee = trade.getSellerFee();
        
        // Покупатель платит totalAmount + buyerFee в quote asset
        buyerWallet.deductReserved(quoteAsset, totalAmount.add(buyerFee));
        buyerWallet.credit(baseAsset, quantity);
        
        // Продавец получает totalAmount - sellerFee в quote asset
        sellerWallet.deductReserved(baseAsset, quantity);
        sellerWallet.credit(quoteAsset, totalAmount.subtract(sellerFee));

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
    @Transactional
    public boolean cancelOrder(String userId, String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Ордер не найден: " + orderId));
        
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Вы не можете отменить чужой ордер");
        }

        if (!order.isActive()) {
            return false;
        }

        String symbol = order.getSymbol();
        Object lock = locks.computeIfAbsent(symbol, k -> new Object());

        synchronized (lock) {
            // Перепроверяем статус после захвата лока
            order = orderRepository.findById(orderId).orElseThrow();
            if (!order.isActive()) {
                return false;
            }

            OrderBook orderBook = getOrderBook(order.getBaseAsset(), order.getQuoteAsset());
            
            // Помечаем как отмененный
            order.cancel();
            
            // Возвращаем зарезервированные средства
            User user = userRepository.findById(userId).orElseThrow();
            releaseOrderReserve(user.getWallet(), order);
            
            orderRepository.save(order);
            userRepository.save(user);

            // Синхронизируем стакан после коммита
            final Order finalOrder = order;
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        orderBook.removeOrder(finalOrder);
                        log.info("Order {} cancelled and removed from OrderBook", finalOrder.getId());
                        marketDataService.broadcastOrderBookUpdate(orderBook);
                    }
                });
            } else {
                orderBook.removeOrder(finalOrder);
                marketDataService.broadcastOrderBookUpdate(orderBook);
            }
            
            return true;
        }
    }

    private void releaseOrderReserve(Wallet wallet, Order order) {
        BigDecimal toRelease;
        String asset;
        
        if (order.getSide() == Side.BUY) {
            asset = order.getQuoteAsset();
            if (order.getType() == OrderType.LIMIT) {
                toRelease = order.getRemainingQuantity().multiply(order.getPrice());
            } else {
                // Для MARKET ордера мы резервировали по Best Ask, 
                // но MARKET ордера обычно исполняются мгновенно и не висят в стакане.
                // Если он все же попал сюда, освобождаем весь остаток резерва.
                toRelease = wallet.getReserved(asset); 
            }
        } else {
            asset = order.getBaseAsset();
            toRelease = order.getRemainingQuantity();
        }
        
        if (toRelease.compareTo(BigDecimal.ZERO) > 0) {
            wallet.unreserve(asset, toRelease);
        }
    }

    /**
     * Возвращает список ордеров пользователя.
     */
    public List<Order> getUserOrders(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Возвращает портфель пользователя (балансы).
     */
    public Wallet getUserWallet(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return user.getWallet();
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
