package com.exchange.engine;

import com.exchange.enums.OrderType;
import com.exchange.enums.Side;
import com.exchange.model.Order;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Стакан заявок (Order Book) для одной торговой пары.
 */
@Getter
public class OrderBook {
    private final String symbol;
    private final NavigableMap<BigDecimal, OrderQueue> bids;
    private final NavigableMap<BigDecimal, OrderQueue> asks;

    public OrderBook(String baseAsset, String quoteAsset) {
        this.symbol = baseAsset + "/" + quoteAsset;
        this.bids = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
        this.asks = new ConcurrentSkipListMap<>();
    }

    public synchronized void addOrder(Order order) {
        if (!order.getSymbol().equals(symbol)) {
            throw new IllegalArgumentException("Symbol mismatch: " + symbol);
        }
        NavigableMap<BigDecimal, OrderQueue> targetMap = order.getSide() == Side.BUY ? bids : asks;
        targetMap.computeIfAbsent(order.getPrice(), k -> new OrderQueue()).addOrder(order);
    }

    public Order getBestBidOrderExcluding(Collection<Order> excluded) {
        return getBestOrderExcluding(bids, excluded);
    }

    public Order getBestAskOrderExcluding(Collection<Order> excluded) {
        return getBestOrderExcluding(asks, excluded);
    }

    private Order getBestOrderExcluding(NavigableMap<BigDecimal, OrderQueue> map, Collection<Order> excluded) {
        for (OrderQueue queue : map.values()) {
            Order best = queue.peekFirstExcluding(excluded);
            if (best != null) return best;
        }
        return null;
    }

    public synchronized void removeOrder(Order order) {
        NavigableMap<BigDecimal, OrderQueue> targetMap = order.getSide() == Side.BUY ? bids : asks;
        OrderQueue queue = targetMap.get(order.getPrice());
        if (queue != null) {
            queue.removeOrder(order);
            if (queue.isEmpty()) targetMap.remove(order.getPrice());
        }
    }

    public boolean hasMatchingOrders(Order order) {
        if (order.getSide() == Side.BUY) {
            BigDecimal bestAsk = getBestAsk();
            return (order.getType() == OrderType.MARKET || order.getPrice() == null) 
                ? !asks.isEmpty() 
                : bestAsk != null && bestAsk.compareTo(order.getPrice()) <= 0;
        } else {
            BigDecimal bestBid = getBestBid();
            return (order.getType() == OrderType.MARKET || order.getPrice() == null) 
                ? !bids.isEmpty() 
                : bestBid != null && bestBid.compareTo(order.getPrice()) >= 0;
        }
    }

    public BigDecimal getBestBid() {
        return bids.isEmpty() ? null : bids.firstKey();
    }

    public BigDecimal getBestAsk() {
        return asks.isEmpty() ? null : asks.firstKey();
    }

    private static class OrderQueue {
        private final java.util.Queue<Order> orders = new java.util.LinkedList<>();

        public synchronized void addOrder(Order order) { orders.offer(order); }
        public synchronized Order peekFirstExcluding(Collection<Order> excluded) {
            return orders.stream().filter(o -> !excluded.contains(o)).findFirst().orElse(null);
        }
        public synchronized void removeOrder(Order order) { orders.remove(order); }
        public synchronized boolean isEmpty() { return orders.isEmpty(); }
        public synchronized int size() { return orders.size(); }
    }
}
