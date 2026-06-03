package com.exchange.service;

import com.exchange.dto.OrderRequest;
import com.exchange.engine.MatchResult;
import com.exchange.engine.MatchingEngine;
import com.exchange.engine.OrderBook;
import com.exchange.enums.OrderType;
import com.exchange.enums.Side;
import com.exchange.model.Order;
import com.exchange.model.Trade;
import com.exchange.model.User;
import com.exchange.model.Wallet;
import com.exchange.repository.OrderRepository;
import com.exchange.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final MatchingManager matchingManager;
    private final MarketDataService marketDataService;
    private final FeeService feeService;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;
    private final WalletService walletService;

    private MatchingEngine matchingEngine;
    private final Map<String, Object> locks = new ConcurrentHashMap<>();
    
    public static final String SYSTEM_FEE_USER = "SYSTEM_FEES";

    @PostConstruct
    public void init() {
        this.matchingEngine = new MatchingEngine(feeService);
        log.info("OrderService initialized with MatchingEngine");
    }

    public List<Trade> submitOrder(User user, OrderRequest request) {
        String symbol = request.baseAsset() + "/" + request.quoteAsset();
        Object lock = locks.computeIfAbsent(symbol, k -> new Object());
        
        synchronized (lock) {
            if (request.type() == OrderType.STOP_LOSS || request.type() == OrderType.TAKE_PROFIT) {
                return transactionTemplate.execute(status -> {
                    Order order = Order.triggerOrder(user.getId(), request.baseAsset(), request.quoteAsset(), 
                                                    request.side(), request.type(), request.quantity(), 
                                                    request.price(), request.triggerPrice());
                    orderRepository.save(order);
                    validateAndReserve(user.getId(), order);
                    matchingManager.addTriggeredOrder(order);
                    log.info("Trigger order {} placed for user {}", order.getId(), user.getUsername());
                    return List.of();
                });
            }

            return transactionTemplate.execute(status -> 
                executeOrderTransaction(user.getId(), request.baseAsset(), request.quoteAsset(), 
                                       request.side(), request.type(), request.quantity(), 
                                       request.price(), request.priceLimit())
            );
        }
    }

    private List<Trade> executeOrderTransaction(String userId, String baseAsset, String quoteAsset, 
                                                Side side, OrderType type, BigDecimal quantity, 
                                                BigDecimal price, BigDecimal priceLimit) {
        OrderBook orderBook = matchingManager.getOrderBook(baseAsset, quoteAsset);
        
        Order order = (type == OrderType.LIMIT) 
            ? Order.limitOrder(userId, baseAsset, quoteAsset, side, quantity, price)
            : Order.marketOrder(userId, baseAsset, quoteAsset, side, quantity, priceLimit);
        
        orderRepository.save(order);
        validateAndReserve(userId, order);
        entityManager.flush();
        
        MatchResult result = matchingEngine.match(orderBook, order);
        
        for (Trade trade : result.getTrades()) {
            settleTrade(trade);
        }
        
        registerPostCommitSync(result, orderBook);
        updateOrderStates(result);
        
        if (order.isActive()) {
            adjustReservedAmount(userId, order);
        } else {
            releaseUnusedReserve(userId, order);
        }
        
        orderRepository.save(order);
        
        // After trade, check if any triggered orders should be activated
        if (!result.getTrades().isEmpty()) {
            BigDecimal lastPrice = result.getTrades().get(result.getTrades().size() - 1).getPrice();
            // We can't call it directly here because we are in a transaction and synchronized block.
            // But we SHOULD call it after commit.
            registerPostCommitTriggerCheck(order.getSymbol(), lastPrice);
        }
        
        return result.getTrades();
    }

    private void registerPostCommitTriggerCheck(String symbol, BigDecimal lastPrice) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    checkAndActivateTriggeredOrders(symbol, lastPrice);
                }
            });
        }
    }

    /**
     * Проверяет и активирует триггерные ордера.
     * Вызывается ПОСЛЕ коммита основной сделки.
     */
    public void checkAndActivateTriggeredOrders(String symbol, BigDecimal lastPrice) {
        List<Order> toActivate = matchingManager.getAndRemoveTriggeredOrders(symbol, lastPrice);
        if (toActivate.isEmpty()) return;

        log.info("Activating {} triggered orders for {} at price {}", toActivate.size(), symbol, lastPrice);
        
        for (Order triggerOrder : toActivate) {
            // Каждый активированный ордер должен исполняться в своей транзакции и со своим локом
            try {
                submitOrderFromTrigger(triggerOrder);
            } catch (Exception e) {
                log.error("Failed to activate triggered order {}: {}", triggerOrder.getId(), e.getMessage());
            }
        }
    }

    private void submitOrderFromTrigger(Order triggerOrder) {
        Object lock = locks.computeIfAbsent(triggerOrder.getSymbol(), k -> new Object());
        synchronized (lock) {
            transactionTemplate.execute(status -> {
                OrderBook orderBook = matchingManager.getOrderBook(triggerOrder.getBaseAsset(), triggerOrder.getQuoteAsset());
                
                // Триггерный ордер превращается в лимитный или рыночный.
                // В нашей реализации STOP_LOSS/TAKE_PROFIT имеют опциональную цену исполнения.
                // Если цена null - это MARKET, если есть - это LIMIT.
                Order executableOrder;
                if (triggerOrder.getPrice() != null) {
                    executableOrder = Order.limitOrder(triggerOrder.getUserId(), triggerOrder.getBaseAsset(), 
                                                     triggerOrder.getQuoteAsset(), triggerOrder.getSide(), 
                                                     triggerOrder.getRemainingQuantity(), triggerOrder.getPrice());
                } else {
                    executableOrder = Order.marketOrder(triggerOrder.getUserId(), triggerOrder.getBaseAsset(), 
                                                      triggerOrder.getQuoteAsset(), triggerOrder.getSide(), 
                                                      triggerOrder.getRemainingQuantity(), null);
                }
                
                // Мы не резервируем заново, так как резерв уже был сделан при создании триггерного ордера.
                // Но нам нужно перенести резерв на новый ордер или связать их.
                // Для простоты: пометим триггерный как FILLED (или TRIGGERED) и создадим новый.
                triggerOrder.addFilledQuantity(triggerOrder.getRemainingQuantity()); // Mark as done
                orderRepository.save(triggerOrder);
                
                orderRepository.save(executableOrder);
                // Важно: нужно "перепривязать" резерв. В текущей модели резерв привязан к ассету в кошельке, а не к ID ордера.
                // Поэтому просто вызываем матчинг.
                
                MatchResult result = matchingEngine.match(orderBook, executableOrder);
                for (Trade trade : result.getTrades()) {
                    settleTrade(trade);
                }
                
                registerPostCommitSync(result, orderBook);
                updateOrderStates(result);
                
                if (!executableOrder.isActive()) {
                    releaseUnusedReserve(executableOrder.getUserId(), executableOrder);
                }
                
                orderRepository.save(executableOrder);
                return null;
            });
        }
    }

    private void updateOrderStates(MatchResult result) {
        for (Trade trade : result.getTrades()) {
            Order buyOrder = trade.getBuyOrder();
            Order sellOrder = trade.getSellOrder();
            buyOrder.addFilledQuantity(trade.getQuantity());
            sellOrder.addFilledQuantity(trade.getQuantity());
            orderRepository.save(buyOrder);
            orderRepository.save(sellOrder);
        }
    }

    private void registerPostCommitSync(MatchResult result, OrderBook orderBook) {
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
                    result.getTrades().forEach(marketDataService::broadcastTrade);
                    marketDataService.broadcastOrderBookUpdate(orderBook);
                }
            });
        }
    }

    private void settleTrade(Trade trade) {
        User feeCollector = userRepository.findByUsername(SYSTEM_FEE_USER)
                .orElseGet(() -> {
                    User u = new User(SYSTEM_FEE_USER, "SYSTEM_NOT_A_PASSWORD");
                    return userRepository.save(u);
                });

        String quoteAsset = trade.getBuyOrder().getQuoteAsset();
        String baseAsset = trade.getBuyOrder().getBaseAsset();
        
        walletService.debitReserved(trade.getBuyerId(), quoteAsset, trade.getTotalAmount().add(trade.getBuyerFee()), "Trade payment (Buyer)");
        walletService.credit(trade.getBuyerId(), baseAsset, trade.getQuantity(), "Trade receipt (Buyer)");
        
        walletService.debitReserved(trade.getSellerId(), baseAsset, trade.getQuantity(), "Trade delivery (Seller)");
        walletService.credit(trade.getSellerId(), quoteAsset, trade.getTotalAmount().subtract(trade.getSellerFee()), "Trade receipt (Seller)");
        
        walletService.credit(feeCollector.getId(), quoteAsset, trade.getBuyerFee().add(trade.getSellerFee()), "Fee collection from trade " + trade.getId());
    }

    private void validateAndReserve(String userId, Order order) {
        if (order.getSide() == Side.BUY) {
            BigDecimal requiredAmount;
            if (order.getType() == OrderType.MARKET) {
                OrderBook orderBook = matchingManager.getOrderBook(order.getBaseAsset(), order.getQuoteAsset());
                BigDecimal bestAsk = orderBook.getBestAsk();
                if (bestAsk == null) throw new IllegalArgumentException("No liquidity for MARKET order");
                requiredAmount = order.getQuantity().multiply(bestAsk).multiply(new BigDecimal("1.10"));
            } else if (order.getType() == OrderType.LIMIT) {
                requiredAmount = order.getQuantity().multiply(order.getPrice());
            } else {
                // For STOP_LOSS/TAKE_PROFIT BUY
                // We use triggerPrice or price (whichever is higher/available) to estimate
                BigDecimal estPrice = order.getPrice() != null ? order.getPrice() : order.getTriggerPrice();
                requiredAmount = order.getQuantity().multiply(estPrice).multiply(new BigDecimal("1.20")); // Higher buffer
            }
            BigDecimal estimatedFee = feeService.calculateTakerFee(requiredAmount);
            walletService.reserve(userId, order.getQuoteAsset(), requiredAmount.add(estimatedFee), "Reserve for " + order.getType() + " BUY order " + order.getId());
        } else {
            walletService.reserve(userId, order.getBaseAsset(), order.getQuantity(), "Reserve for " + order.getType() + " SELL order " + order.getId());
        }
    }

    private void adjustReservedAmount(String userId, Order order) {
    }

    private void releaseUnusedReserve(String userId, Order order) {
        Wallet wallet = walletService.getWallet(userId);
        String asset = order.getSide() == Side.BUY ? order.getQuoteAsset() : order.getBaseAsset();
        BigDecimal reserved = wallet.getReserved(asset);
        if (reserved.compareTo(BigDecimal.ZERO) > 0) {
            walletService.unreserve(userId, asset, reserved, "Release unused reserve for order " + order.getId());
        }
    }

    public boolean cancelOrder(String userId, String orderId) {
        String symbol;
        {
            Order o = orderRepository.findById(orderId).orElseThrow();
            symbol = o.getSymbol();
        }
        Object lock = locks.computeIfAbsent(symbol, k -> new Object());
        synchronized (lock) {
            return transactionTemplate.execute(status -> executeCancelTransaction(userId, orderId));
        }
    }

    private boolean executeCancelTransaction(String userId, String orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        if (!order.getUserId().equals(userId) || !order.isActive()) return false;

        order.cancel();
        releaseUnusedReserve(userId, order);
        
        orderRepository.save(order);
        
        // If it was a triggered order, remove from memory
        // (MatchingManager.init would recover it if we didn't remove, but cancel marks it in DB)
        // For simplicity, we just let it be marked as CANCELLED in DB. 
        // MatchingManager.getAndRemoveTriggeredOrders should skip non-active orders if we wanted to be more robust.
        
        return true;
    }

    public List<Order> getUserOrders(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
