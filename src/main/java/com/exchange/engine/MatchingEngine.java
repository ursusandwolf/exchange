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
 * Отвечает за расчет сделок. Не изменяет OrderBook напрямую, 
 * а возвращает MatchResult с инструкциями по обновлению.
 */
public class MatchingEngine {
    
    /**
     * Вычисляет сделки для входящего ордера.
     * НЕ изменяет состояние стакана.
     */
    public MatchResult match(OrderBook orderBook, Order incomingOrder) {
        List<Trade> trades = new ArrayList<>();
        List<Order> ordersToRemove = new ArrayList<>();
        
        // Клонируем входящий ордер для расчетов (чтобы не менять оригинал раньше времени)
        // Но в этой реализации мы будем аккуратно менять оригинал, 
        // так как он еще не в стакане.
        
        while (incomingOrder.isActive() && orderBook.hasMatchingOrders(incomingOrder)) {
            Order contraOrder = getContraOrder(orderBook, incomingOrder, ordersToRemove);
            if (contraOrder == null || !contraOrder.isActive()) {
                break;
            }
            
            Trade trade = calculateTrade(incomingOrder, contraOrder);
            if (trade != null) {
                trades.add(trade);
                if (!contraOrder.isActive()) {
                    ordersToRemove.add(contraOrder);
                }
            } else {
                break;
            }
        }
        
        Order remainingOrder = incomingOrder.isActive() ? incomingOrder : null;
        
        return new MatchResult(trades, ordersToRemove, remainingOrder);
    }

    private Order getContraOrder(OrderBook orderBook, Order incomingOrder, List<Order> alreadyRemoved) {
        // Нам нужно пропустить те, что мы уже пометили на удаление в этом цикле
        if (incomingOrder.getSide() == Side.BUY) {
            return orderBook.getBestAskOrderExcluding(alreadyRemoved);
        } else {
            return orderBook.getBestBidOrderExcluding(alreadyRemoved);
        }
    }

    private Trade calculateTrade(Order incomingOrder, Order contraOrder) {
        BigDecimal tradePrice = determineTradePrice(incomingOrder, contraOrder);
        
        // Slippage Protection check
        if (incomingOrder.getType() == OrderType.MARKET && incomingOrder.getPriceLimit() != null) {
            if (incomingOrder.getSide() == Side.BUY && tradePrice.compareTo(incomingOrder.getPriceLimit()) > 0) {
                return null; // Price too high for buyer
            }
            if (incomingOrder.getSide() == Side.SELL && tradePrice.compareTo(incomingOrder.getPriceLimit()) < 0) {
                return null; // Price too low for seller
            }
        }

        BigDecimal tradeQuantity = incomingOrder.getRemainingQuantity().min(contraOrder.getRemainingQuantity());
        
        if (tradeQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        
        Order buyOrder = incomingOrder.getSide() == Side.BUY ? incomingOrder : contraOrder;
        Order sellOrder = incomingOrder.getSide() == Side.SELL ? incomingOrder : contraOrder;
        
        Trade trade = new Trade(buyOrder, sellOrder, tradePrice, tradeQuantity);
        
        // Обновляем количество (пока в памяти объекта Order)
        incomingOrder.addFilledQuantity(tradeQuantity);
        contraOrder.addFilledQuantity(tradeQuantity);
        
        return trade;
    }

    private BigDecimal determineTradePrice(Order order, Order contraOrder) {
        if (order.getType() == OrderType.MARKET) return contraOrder.getPrice();
        if (contraOrder.getType() == OrderType.MARKET) return order.getPrice();
        return contraOrder.getPrice(); // Maker price
    }
}
