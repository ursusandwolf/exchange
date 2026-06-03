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
import com.exchange.service.FeeService;

public class MatchingEngine {
    private final FeeService feeService;

    public MatchingEngine(FeeService feeService) {
        this.feeService = feeService;
    }
    
    /**
     * Вычисляет сделки для входящего ордера.
     * НЕ изменяет состояние стакана и НЕ изменяет состояние ордеров напрямую.
     */
    public MatchResult match(OrderBook orderBook, Order incomingOrder) {
        List<Trade> trades = new ArrayList<>();
        List<Order> ordersToRemove = new ArrayList<>();
        
        // Создаем копии для расчетов, чтобы не менять оригиналы в памяти до коммита
        BigDecimal remainingQty = incomingOrder.getRemainingQuantity();
        
        while (remainingQty.compareTo(BigDecimal.ZERO) > 0 && orderBook.hasMatchingOrders(incomingOrder)) {
            Order contraOrder = getContraOrder(orderBook, incomingOrder, ordersToRemove);
            if (contraOrder == null || !contraOrder.isActive()) {
                break;
            }
            
            // Нам нужно знать, сколько УЖЕ заполнено в этом цикле для contraOrder
            BigDecimal contraRemainingQty = contraOrder.getRemainingQuantity();
            
            Trade trade = calculateTrade(incomingOrder, contraOrder, remainingQty, contraRemainingQty);
            if (trade != null) {
                trades.add(trade);
                remainingQty = remainingQty.subtract(trade.getQuantity());
                
                // Если contraOrder полностью заполнен в результате этой сделки (или серии сделок в этом цикле)
                // На самом деле нам нужно отслеживать суммарное заполнение contraOrder в этом цикле, 
                // но так как мы берем новый contraOrder каждый раз, или тот же если он не заполнен...
                // В этой простой реализации contraOrder заполняется за один раз или становится полностью заполненным.
                if (trade.getQuantity().compareTo(contraRemainingQty) >= 0) {
                    ordersToRemove.add(contraOrder);
                }
            } else {
                break;
            }
        }
        
        return new MatchResult(trades, ordersToRemove, remainingQty.compareTo(BigDecimal.ZERO) > 0 ? incomingOrder : null);
    }

    private Order getContraOrder(OrderBook orderBook, Order incomingOrder, List<Order> alreadyRemoved) {
        if (incomingOrder.getSide() == Side.BUY) {
            return orderBook.getBestAskOrderExcluding(alreadyRemoved);
        } else {
            return orderBook.getBestBidOrderExcluding(alreadyRemoved);
        }
    }

    private Trade calculateTrade(Order incomingOrder, Order contraOrder, BigDecimal incomingRemaining, BigDecimal contraRemaining) {
        BigDecimal tradePrice = determineTradePrice(incomingOrder, contraOrder);
        
        // Slippage Protection check
        if (incomingOrder.getType() == OrderType.MARKET && incomingOrder.getPriceLimit() != null) {
            if (incomingOrder.getSide() == Side.BUY && tradePrice.compareTo(incomingOrder.getPriceLimit()) > 0) {
                return null;
            }
            if (incomingOrder.getSide() == Side.SELL && tradePrice.compareTo(incomingOrder.getPriceLimit()) < 0) {
                return null;
            }
        }

        BigDecimal tradeQuantity = incomingRemaining.min(contraRemaining);
        
        if (tradeQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        
        Order buyOrder = incomingOrder.getSide() == Side.BUY ? incomingOrder : contraOrder;
        Order sellOrder = incomingOrder.getSide() == Side.SELL ? incomingOrder : contraOrder;
        
        BigDecimal totalAmount = tradePrice.multiply(tradeQuantity);
        BigDecimal buyerFee = (incomingOrder.getSide() == Side.BUY) 
                ? feeService.calculateTakerFee(totalAmount) 
                : feeService.calculateMakerFee(totalAmount);
        BigDecimal sellerFee = (incomingOrder.getSide() == Side.SELL) 
                ? feeService.calculateTakerFee(totalAmount) 
                : feeService.calculateMakerFee(totalAmount);

        return new Trade(buyOrder, sellOrder, tradePrice, tradeQuantity, 
                                buyerFee, sellerFee, incomingOrder.getId(), contraOrder.getId());
    }

    private BigDecimal determineTradePrice(Order order, Order contraOrder) {
        if (order.getType() == OrderType.MARKET) return contraOrder.getPrice();
        if (contraOrder.getType() == OrderType.MARKET) return order.getPrice();
        return contraOrder.getPrice(); // Maker price
    }
}
