package com.exchange.service;

import com.alex.fin.core.domain.common.InstrumentId;
import com.alex.fin.core.domain.common.Price;
import com.alex.fin.core.domain.common.Quantity;
import com.alex.fin.core.domain.common.CurrencyCode;
import com.exchange.dto.OcoOrderRequest;
import com.exchange.dto.OcoOrderResponse;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

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
            InstrumentId baseId = new InstrumentId(request.baseAsset());
            InstrumentId quoteId = new InstrumentId(request.quoteAsset());
            CurrencyCode quoteCurrency = new CurrencyCode(request.quoteAsset());
            Quantity quantity = new Quantity(request.quantity());
            Price price = request.price() != null ? new Price(request.price(), quoteCurrency) : null;
            Price triggerPrice = request.triggerPrice() != null ? new Price(request.triggerPrice(), quoteCurrency) : null;
            Price priceLimit = request.priceLimit() != null ? new Price(request.priceLimit(), quoteCurrency) : null;

            if (request.type() == OrderType.STOP_LOSS || request.type() == OrderType.TAKE_PROFIT) {
                return transactionTemplate.execute(status -> {
                    Order order = Order.triggerOrder(user.getId(), baseId, quoteId, 
                                                    request.side(), request.type(), quantity, 
                                                    price, triggerPrice);
                    orderRepository.save(order);
                    validateAndReserve(user.getId(), order);
                    matchingManager.addTriggeredOrder(order);
                    log.info("Trigger order {} placed for user {}", order.getId(), user.getUsername());
                    return List.of();
                });
            }

            return transactionTemplate.execute(status -> 
                executeOrderTransaction(user.getId(), baseId, quoteId, 
                                       request.side(), request.type(), quantity, 
                                       price, priceLimit)
            );
        }
    }

    public OcoOrderResponse submitOcoOrder(User user, OcoOrderRequest request) {
        if (request.side() != Side.SELL) {
            throw new IllegalArgumentException("OCO orders currently support SELL side only");
        }
        if (request.takeProfitPrice().compareTo(request.stopLossTriggerPrice()) <= 0) {
            throw new IllegalArgumentException("Take profit price must be above stop loss trigger price for OCO SELL orders");
        }

        String symbol = request.baseAsset() + "/" + request.quoteAsset();
        Object lock = locks.computeIfAbsent(symbol, k -> new Object());

        synchronized (lock) {
            return transactionTemplate.execute(status -> {
                InstrumentId baseId = new InstrumentId(request.baseAsset());
                InstrumentId quoteId = new InstrumentId(request.quoteAsset());
                CurrencyCode quoteCurrency = new CurrencyCode(request.quoteAsset());
                Quantity quantity = new Quantity(request.quantity());
                String groupId = UUID.randomUUID().toString();

                walletService.reserve(
                        user.getId(),
                        request.baseAsset(),
                        request.quantity(),
                        "Reserve for OCO group " + groupId
                );

                Order takeProfitOrder = Order.triggerOrder(
                        user.getId(),
                        baseId,
                        quoteId,
                        Side.SELL,
                        OrderType.TAKE_PROFIT,
                        quantity,
                        new Price(request.takeProfitPrice(), quoteCurrency),
                        new Price(request.takeProfitPrice(), quoteCurrency)
                );
                takeProfitOrder.linkOcoGroup(groupId);

                Order stopLossOrder = Order.triggerOrder(
                        user.getId(),
                        baseId,
                        quoteId,
                        Side.SELL,
                        OrderType.STOP_LOSS,
                        quantity,
                        request.stopLossPrice() != null ? new Price(request.stopLossPrice(), quoteCurrency) : null,
                        new Price(request.stopLossTriggerPrice(), quoteCurrency)
                );
                stopLossOrder.linkOcoGroup(groupId);

                orderRepository.save(takeProfitOrder);
                orderRepository.save(stopLossOrder);

                matchingManager.addTriggeredOrder(takeProfitOrder);
                matchingManager.addTriggeredOrder(stopLossOrder);

                log.info("OCO group {} placed for user {}", groupId, user.getUsername());
                return new OcoOrderResponse(groupId, List.of(takeProfitOrder.getId(), stopLossOrder.getId()));
            });
        }
    }

    private List<Trade> executeOrderTransaction(String userId, InstrumentId baseAsset, InstrumentId quoteAsset, 
                                                Side side, OrderType type, Quantity quantity, 
                                                Price price, Price priceLimit) {
        OrderBook orderBook = matchingManager.getOrderBook(baseAsset.value(), quoteAsset.value());
        
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
            Price lastPrice = result.getTrades().get(result.getTrades().size() - 1).getPrice();
            // We can't call it directly here because we are in a transaction and synchronized block.
            // But we SHOULD call it after commit.
            registerPostCommitTriggerCheck(order.getSymbol(), lastPrice.value());
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
                OrderBook orderBook = matchingManager.getOrderBook(triggerOrder.getBaseAsset().value(), triggerOrder.getQuoteAsset().value());
                
                // Триггерный ордер превращается в лимитный или рыночный.
                Order executableOrder;
                if (triggerOrder.getPrice() != null) {
                    executableOrder = Order.limitOrder(triggerOrder.getUserId(), triggerOrder.getBaseAsset(), 
                                                     triggerOrder.getQuoteAsset(), triggerOrder.getSide(), 
                                                     new Quantity(triggerOrder.getRemainingQuantity()), triggerOrder.getPrice());
                } else {
                    executableOrder = Order.marketOrder(triggerOrder.getUserId(), triggerOrder.getBaseAsset(), 
                                                      triggerOrder.getQuoteAsset(), triggerOrder.getSide(), 
                                                      new Quantity(triggerOrder.getRemainingQuantity()), null);
                }

                if (triggerOrder.getOcoGroupId() != null) {
                    executableOrder.linkOcoGroup(triggerOrder.getOcoGroupId());
                }
                
                triggerOrder.addFilledQuantity(triggerOrder.getRemainingQuantity()); // Mark as done
                orderRepository.save(triggerOrder);
                
                orderRepository.save(executableOrder);

                if (triggerOrder.getOcoGroupId() != null) {
                    cancelOcoGroupOrders(triggerOrder.getUserId(), triggerOrder.getOcoGroupId(), triggerOrder.getId(), executableOrder.getId());
                }
                
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

    private boolean cancelOcoGroupOrders(String userId, String ocoGroupId, String... excludedOrderIds) {
        if (ocoGroupId == null) {
            return false;
        }

        HashSet<String> excluded = new HashSet<>(List.of(excludedOrderIds));
        List<Order> siblings = orderRepository.findByOcoGroupId(ocoGroupId);
        boolean activated = siblings.stream().anyMatch(order ->
                order.getType() == OrderType.LIMIT || order.getType() == OrderType.MARKET
        );

        for (Order sibling : siblings) {
            if (excluded.contains(sibling.getId()) || !sibling.isActive()) {
                continue;
            }

            if (sibling.getType() == OrderType.LIMIT || sibling.getType() == OrderType.MARKET) {
                OrderBook orderBook = matchingManager.getOrderBookBySymbol(sibling.getSymbol());
                if (orderBook != null) {
                    orderBook.removeOrder(sibling);
                }
                releaseUnusedReserve(userId, sibling);
            } else {
                matchingManager.removeTriggeredOrder(sibling.getId());
                if (activated) {
                    // OCO reserve is released only once on the executable leg.
                }
            }

            sibling.cancel();
            orderRepository.save(sibling);
        }

        return activated;
    }

    private void updateOrderStates(MatchResult result) {
        for (Trade trade : result.getTrades()) {
            Order buyOrder = trade.getBuyOrder();
            Order sellOrder = trade.getSellOrder();
            buyOrder.addFilledQuantity(trade.getQuantity().value());
            sellOrder.addFilledQuantity(trade.getQuantity().value());
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

        String quoteAsset = trade.getBuyOrder().getQuoteAsset().value();
        String baseAsset = trade.getBuyOrder().getBaseAsset().value();
        
        walletService.debitReserved(trade.getBuyerId(), quoteAsset, trade.getTotalAmount().add(trade.getBuyerFee()), "Trade payment (Buyer)");
        walletService.credit(trade.getBuyerId(), baseAsset, trade.getQuantity().value(), "Trade receipt (Buyer)");
        
        walletService.debitReserved(trade.getSellerId(), baseAsset, trade.getQuantity().value(), "Trade delivery (Seller)");
        walletService.credit(trade.getSellerId(), quoteAsset, trade.getTotalAmount().subtract(trade.getSellerFee()), "Trade receipt (Seller)");
        
        walletService.credit(feeCollector.getId(), quoteAsset, trade.getBuyerFee().add(trade.getSellerFee()), "Fee collection from trade " + trade.getId());
    }

    private void validateAndReserve(String userId, Order order) {
        if (order.getSide() == Side.BUY) {
            BigDecimal requiredAmount;
            if (order.getType() == OrderType.MARKET) {
                OrderBook orderBook = matchingManager.getOrderBook(order.getBaseAsset().value(), order.getQuoteAsset().value());
                BigDecimal bestAsk = orderBook.getBestAsk();
                if (bestAsk == null) throw new IllegalArgumentException("No liquidity for MARKET order");
                requiredAmount = order.getQuantity().value().multiply(bestAsk).multiply(new BigDecimal("1.10"));
            } else if (order.getType() == OrderType.LIMIT) {
                requiredAmount = order.getQuantity().value().multiply(order.getPrice().value());
            } else {
                BigDecimal estPriceVal = order.getPrice() != null ? order.getPrice().value() : order.getTriggerPrice().value();
                requiredAmount = order.getQuantity().value().multiply(estPriceVal).multiply(new BigDecimal("1.20")); // Higher buffer
            }
            BigDecimal estimatedFee = feeService.calculateTakerFee(requiredAmount);
            walletService.reserve(userId, order.getQuoteAsset().value(), requiredAmount.add(estimatedFee), "Reserve for " + order.getType() + " BUY order " + order.getId());
        } else {
            walletService.reserve(userId, order.getBaseAsset().value(), order.getQuantity().value(), "Reserve for " + order.getType() + " SELL order " + order.getId());
        }
    }

    private void adjustReservedAmount(String userId, Order order) {
    }

    private void releaseUnusedReserve(String userId, Order order) {
        Wallet wallet = walletService.getWallet(userId);
        String asset = order.getSide() == Side.BUY ? order.getQuoteAsset().value() : order.getBaseAsset().value();
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

        if (order.getOcoGroupId() != null) {
            boolean activated = cancelOcoGroupOrders(userId, order.getOcoGroupId());
            if (!activated) {
                walletService.unreserve(
                        userId,
                        order.getBaseAsset().value(),
                        order.getQuantity().value(),
                        "Release OCO reserve for group " + order.getOcoGroupId()
                );
            }
            return true;
        }

        order.cancel();
        releaseUnusedReserve(userId, order);

        if (order.getType() == OrderType.STOP_LOSS || order.getType() == OrderType.TAKE_PROFIT) {
            matchingManager.removeTriggeredOrder(order.getId());
        } else {
            OrderBook orderBook = matchingManager.getOrderBookBySymbol(order.getSymbol());
            if (orderBook != null) {
                orderBook.removeOrder(order);
            }
        }

        orderRepository.save(order);

        return true;
    }

    public List<Order> getUserOrders(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
