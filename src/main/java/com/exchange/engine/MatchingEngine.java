package com.exchange.engine;

import com.exchange.enums.OrderType;
import com.exchange.enums.Side;
import com.exchange.model.Order;
import com.exchange.model.Trade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Matching Engine — движок сведения ордеров.
 * Отвечает за исполнение сделок между ордерами на покупку и продажу.
 * 
 * Правила matching:
 * 1. Если цена BUY >= лучшей цены SELL — сделка исполняется
 * 2. MARKET ордера исполняются по лучшей доступной цене
 * 3. Частичное исполнение разрешено
 * 4. Приоритет: цена, затем время (FIFO внутри одной цены)
 */
public class MatchingEngine {
    
    /**
     * Обрабатывает новый ордер и возвращает список совершённых сделок.
     */
    public synchronized List<Trade> match(OrderBook orderBook, Order incomingOrder) {
        List<Trade> trades = new ArrayList<>();
        
        // Пока ордер активен и есть ордера для сведения
        while (incomingOrder.isActive() && orderBook.hasMatchingOrders(incomingOrder)) {
            Order contraOrder = getContraOrder(orderBook, incomingOrder);
            if (contraOrder == null || !contraOrder.isActive()) {
                break;
            }
            
            Trade trade = executeTrade(orderBook, incomingOrder, contraOrder);
            if (trade != null) {
                trades.add(trade);
            }
        }
        
        // Если ордер всё ещё активен после matching — добавляем его в стакан
        if (incomingOrder.isActive()) {
            orderBook.addOrder(incomingOrder);
        }
        
        return trades;
    }

    /**
     * Получает противоположный ордер из стакана.
     */
    private Order getContraOrder(OrderBook orderBook, Order incomingOrder) {
        if (incomingOrder.getSide() == Side.BUY) {
            return orderBook.getBestAskOrder();
        } else {
            return orderBook.getBestBidOrder();
        }
    }

    /**
     * Исполняет сделку между двумя ордерами.
     * 
     * @return Совершённая сделка или null, если сделка не состоялась
     */
    private Trade executeTrade(OrderBook orderBook, Order incomingOrder, Order contraOrder) {
        // Определяем цену исполнения
        BigDecimal tradePrice = determineTradePrice(incomingOrder, contraOrder);
        
        // Определяем объём сделки (минимум из оставшихся количеств обоих ордеров)
        BigDecimal incomingRemaining = incomingOrder.getRemainingQuantity();
        BigDecimal contraRemaining = contraOrder.getRemainingQuantity();
        BigDecimal tradeQuantity = incomingRemaining.min(contraRemaining);
        
        if (tradeQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        
        // Создаём сделку
        Order buyOrder = incomingOrder.getSide() == Side.BUY ? incomingOrder : contraOrder;
        Order sellOrder = incomingOrder.getSide() == Side.SELL ? incomingOrder : contraOrder;
        
        Trade trade = new Trade(buyOrder, sellOrder, tradePrice, tradeQuantity);
        
        // Обновляем статусы ордеров
        incomingOrder.addFilledQuantity(tradeQuantity);
        contraOrder.addFilledQuantity(tradeQuantity);
        
        // Если контрактный ордер полностью исполнен — удаляем из стакана
        if (!contraOrder.isActive()) {
            orderBook.removeOrder(contraOrder);
        }
        
        return trade;
    }

    /**
     * Определяет цену исполнения сделки.
     * - Для LIMIT ордеров: цена контрактного ордера (цена в стакане)
     * - Для MARKET ордеров: лучшая доступная цена в стакане
     */
    private BigDecimal determineTradePrice(Order order, Order contraOrder) {
        // Если входящий ордер MARKET — берём цену контрактного ордера
        if (order.getType() == OrderType.MARKET) {
            return contraOrder.getPrice();
        }
        
        // Если контрактный ордер MARKET (маловероятно, но возможно) — берём цену входящего
        if (contraOrder.getType() == OrderType.MARKET) {
            return order.getPrice();
        }
        
        // Оба LIMIT: берём цену того ордера, который был в стакане раньше
        // В упрощённой версии берём цену контрактного ордера (maker price)
        return contraOrder.getPrice();
    }

    /**
     * Отменяет ордер в стакане (если он там есть).
     * Возвращает true, если ордер был найден и удалён.
     */
    public boolean cancelOrder(OrderBook orderBook, Order order) {
        if (!order.isActive()) {
            return false;
        }
        
        orderBook.removeOrder(order);
        return true;
    }
}
