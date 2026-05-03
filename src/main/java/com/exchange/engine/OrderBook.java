package com.exchange.engine;

import com.exchange.enums.OrderType;
import com.exchange.enums.Side;
import com.exchange.model.Order;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Стакан заявок (Order Book) для одной торговой пары.
 * Хранит ордера на покупку (bids) и продажу (asks), отсортированные по цене.
 * 
 * - Bids (покупки): сортируются по убыванию цены (лучшая цена — самая высокая)
 * - Asks (продажи): сортируются по возрастанию цены (лучшая цена — самая низкая)
 */
@Getter
public class OrderBook {
    private final String symbol;  // Торговая пара, например "BTC/USDT"
    
    // Bids: price -> список ордеров (лучшая цена максимальная)
    private final NavigableMap<BigDecimal, OrderQueue> bids;
    
    // Asks: price -> список ордеров (лучшая цена минимальная)
    private final NavigableMap<BigDecimal, OrderQueue> asks;

    public OrderBook(String baseAsset, String quoteAsset) {
        this.symbol = baseAsset + "/" + quoteAsset;
        // Reverse order для bids (убывание цен)
        this.bids = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
        // Natural order для asks (возрастание цен)
        this.asks = new ConcurrentSkipListMap<>();
    }

    /**
     * Добавляет ордер в стакан.
     */
    public synchronized void addOrder(Order order) {
        if (!order.getSymbol().equals(symbol)) {
            throw new IllegalArgumentException("Ордер не соответствует торговой паре стакана: " + symbol);
        }
        
        NavigableMap<BigDecimal, OrderQueue> targetMap = 
            order.getSide() == Side.BUY ? bids : asks;
        
        targetMap.computeIfAbsent(order.getPrice(), k -> new OrderQueue())
                 .addOrder(order);
    }

    /**
     * Возвращает лучшую цену покупки (максимальный bid).
     */
    public BigDecimal getBestBid() {
        Map.Entry<BigDecimal, OrderQueue> entry = bids.firstEntry();
        return entry != null ? entry.getKey() : null;
    }

    /**
     * Возвращает лучшую цену продажи (минимальный ask).
     */
    public BigDecimal getBestAsk() {
        Map.Entry<BigDecimal, OrderQueue> entry = asks.firstEntry();
        return entry != null ? entry.getKey() : null;
    }

    /**
     * Возвращает лучший ордер на покупку с указанным объёмом доступным для исполнения.
     */
    public Order getBestBidOrder() {
        Map.Entry<BigDecimal, OrderQueue> entry = bids.firstEntry();
        return entry != null ? entry.getValue().peekFirst() : null;
    }

    /**
     * Возвращает лучший ордер на продажу с указанным объёмом доступным для исполнения.
     */
    public Order getBestAskOrder() {
        Map.Entry<BigDecimal, OrderQueue> entry = asks.firstEntry();
        return entry != null ? entry.getValue().peekFirst() : null;
    }

    /**
     * Удаляет исполненный или частично исполненный ордер из стакана.
     * Если ордер полностью исполнен — удаляется целиком.
     * Если частично — обновляется оставшийся объём.
     */
    public synchronized void removeOrder(Order order) {
        NavigableMap<BigDecimal, OrderQueue> targetMap = 
            order.getSide() == Side.BUY ? bids : asks;
        
        OrderQueue queue = targetMap.get(order.getPrice());
        if (queue != null) {
            queue.removeOrder(order);
            if (queue.isEmpty()) {
                targetMap.remove(order.getPrice());
            }
        }
    }

    /**
     * Проверяет, есть ли ордера, которые могут быть исполнены против данного ордера.
     */
    public boolean hasMatchingOrders(Order order) {
        if (order.getSide() == Side.BUY) {
            if (order.getType() == OrderType.MARKET || order.getPrice() == null) {
                return !asks.isEmpty();
            }
            BigDecimal bestAsk = getBestAsk();
            return bestAsk != null && bestAsk.compareTo(order.getPrice()) <= 0;
        } else { // SELL
            if (order.getType() == OrderType.MARKET || order.getPrice() == null) {
                return !bids.isEmpty();
            }
            BigDecimal bestBid = getBestBid();
            return bestBid != null && bestBid.compareTo(order.getPrice()) >= 0;
        }
    }

    /**
     * Возвращает количество ордеров на покупку.
     */
    public int getBidCount() {
        return bids.values().stream().mapToInt(OrderQueue::size).sum();
    }

    /**
     * Возвращает количество ордеров на продажу.
     */
    public int getAskCount() {
        return asks.values().stream().mapToInt(OrderQueue::size).sum();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("OrderBook{symbol='").append(symbol).append("'\n");
        sb.append("Bids (buy):\n");
        for (Map.Entry<BigDecimal, OrderQueue> entry : bids.entrySet()) {
            sb.append("  Price: ").append(entry.getKey()).append(", Orders: ").append(entry.getValue().size()).append("\n");
        }
        sb.append("Asks (sell):\n");
        for (Map.Entry<BigDecimal, OrderQueue> entry : asks.entrySet()) {
            sb.append("  Price: ").append(entry.getKey()).append(", Orders: ").append(entry.getValue().size()).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Внутренний класс для хранения очереди ордеров по одной цене.
     * Использует FIFO принцип для ордеров с одинаковой ценой.
     */
    private static class OrderQueue {
        private final java.util.Queue<Order> orders = new java.util.LinkedList<>();

        public void addOrder(Order order) {
            orders.offer(order);
        }

        public Order peekFirst() {
            return orders.peek();
        }

        public Order pollFirst() {
            return orders.poll();
        }

        public void removeOrder(Order order) {
            orders.remove(order);
        }

        public boolean isEmpty() {
            return orders.isEmpty();
        }

        public int size() {
            return orders.size();
        }
    }
}
