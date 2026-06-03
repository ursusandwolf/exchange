package com.exchange.service;

import com.exchange.engine.OrderBook;
import com.exchange.model.Order;
import com.exchange.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingManager {
    private final OrderRepository orderRepository;
    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    private final Map<String, List<Order>> triggeredOrders = new ConcurrentHashMap<>();

    @jakarta.annotation.PostConstruct
    @Transactional(readOnly = true)
    public void init() {
        log.info("Starting OrderBook recovery from database...");
        List<Order> activeOrders = orderRepository.findByStatusInOrderByCreatedAtAsc(
                List.of(com.exchange.enums.OrderStatus.PENDING, com.exchange.enums.OrderStatus.PARTIALLY_FILLED)
        );
        
        for (Order order : activeOrders) {
            if (order.getType() == com.exchange.enums.OrderType.LIMIT || order.getType() == com.exchange.enums.OrderType.MARKET) {
                OrderBook orderBook = getOrderBook(order.getBaseAsset(), order.getQuoteAsset());
                orderBook.addOrder(order);
            } else {
                addTriggeredOrder(order);
            }
        }
        log.info("Recovered {} orders into books and {} triggered orders", activeOrders.size(), triggeredOrders.size());
    }

    public void addTriggeredOrder(Order order) {
        triggeredOrders.computeIfAbsent(order.getSymbol(), k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(order);
    }

    public List<Order> getAndRemoveTriggeredOrders(String symbol, BigDecimal lastPrice) {
        List<Order> triggered = triggeredOrders.get(symbol);
        if (triggered == null || triggered.isEmpty()) return List.of();

        List<Order> toActivate = new java.util.ArrayList<>();
        triggered.removeIf(order -> {
            boolean shouldTrigger = false;
            if (order.getType() == com.exchange.enums.OrderType.STOP_LOSS) {
                // Stop Loss: trigger when price goes BELOW (for sell) or ABOVE (for buy) triggerPrice?
                // Standard: Sell Stop Loss triggers when price hits or goes below.
                // Buy Stop Loss triggers when price hits or goes above.
                if (order.getSide() == com.exchange.enums.Side.SELL && lastPrice.compareTo(order.getTriggerPrice()) <= 0) shouldTrigger = true;
                if (order.getSide() == com.exchange.enums.Side.BUY && lastPrice.compareTo(order.getTriggerPrice()) >= 0) shouldTrigger = true;
            } else if (order.getType() == com.exchange.enums.OrderType.TAKE_PROFIT) {
                // Take Profit: Sell triggers when price hits or goes above.
                if (order.getSide() == com.exchange.enums.Side.SELL && lastPrice.compareTo(order.getTriggerPrice()) >= 0) shouldTrigger = true;
                if (order.getSide() == com.exchange.enums.Side.BUY && lastPrice.compareTo(order.getTriggerPrice()) <= 0) shouldTrigger = true;
            }
            
            if (shouldTrigger) {
                toActivate.add(order);
                return true;
            }
            return false;
        });
        return toActivate;
    }

    public OrderBook getOrderBook(String baseAsset, String quoteAsset) {
        String symbol = baseAsset + "/" + quoteAsset;
        return orderBooks.computeIfAbsent(symbol, k -> new OrderBook(baseAsset, quoteAsset));
    }

    public List<String> getSymbols() {
        return new ArrayList<>(orderBooks.keySet());
    }

    public OrderBook getOrderBookBySymbol(String symbol) {
        return orderBooks.get(symbol);
    }
}
