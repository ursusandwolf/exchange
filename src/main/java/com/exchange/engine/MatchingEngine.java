package com.exchange.engine;

import com.alex.fin.core.domain.common.Price;
import com.alex.fin.core.domain.common.Quantity;
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
                remainingQty = remainingQty.subtract(trade.getQuantity().value());
                
                // Если contraOrder полностью заполнен в результате этой сделки (или серии сделок в этом цикле)
                if (trade.getQuantity().value().compareTo(contraRemainingQty) >= 0) {
                    ordersToRemove.add(contraOrder);
                }
            } else {
                break;
            }
        }
        
        return new MatchResult(trades, ordersToRemove, remainingQty.compareTo(BigDecimal.ZERO) > 0 ? incomingOrder : null);
    }

    private Order getContraOrder(OrderBook orderBook, Order incomingOrder, List<Order> alreadyRemoved) {
        while (true) {
            Order candidate = incomingOrder.getSide() == Side.BUY
                    ? orderBook.getBestAskOrderExcluding(alreadyRemoved)
                    : orderBook.getBestBidOrderExcluding(alreadyRemoved);

            if (candidate == null) {
                return null;
            }
            if (candidate.isActive()) {
                return candidate;
            }
            alreadyRemoved.add(candidate);
        }
    }

    private Trade calculateTrade(Order incomingOrder, Order contraOrder, BigDecimal incomingRemaining, BigDecimal contraRemaining) {
        Price tradePrice = determineTradePrice(incomingOrder, contraOrder);
        
        // Slippage Protection check
        if (incomingOrder.getType() == OrderType.MARKET && incomingOrder.getPriceLimit() != null) {
            if (incomingOrder.getSide() == Side.BUY && tradePrice.value().compareTo(incomingOrder.getPriceLimit().value()) > 0) {
                return null;
            }
            if (incomingOrder.getSide() == Side.SELL && tradePrice.value().compareTo(incomingOrder.getPriceLimit().value()) < 0) {
                return null;
            }
        }

        BigDecimal tradeQtyVal = incomingRemaining.min(contraRemaining);
        
        if (tradeQtyVal.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        Quantity tradeQuantity = new Quantity(tradeQtyVal);
        
        Order buyOrder = incomingOrder.getSide() == Side.BUY ? incomingOrder : contraOrder;
        Order sellOrder = incomingOrder.getSide() == Side.SELL ? incomingOrder : contraOrder;
        
        BigDecimal totalAmount = tradePrice.value().multiply(tradeQuantity.value());
        BigDecimal buyerFee = (incomingOrder.getSide() == Side.BUY) 
                ? feeService.calculateTakerFee(totalAmount) 
                : feeService.calculateMakerFee(totalAmount);
        BigDecimal sellerFee = (incomingOrder.getSide() == Side.SELL) 
                ? feeService.calculateTakerFee(totalAmount) 
                : feeService.calculateMakerFee(totalAmount);

        return new Trade(buyOrder, sellOrder, tradePrice, tradeQuantity, 
                                buyerFee, sellerFee, incomingOrder.getId(), contraOrder.getId());
    }

    private Price determineTradePrice(Order order, Order contraOrder) {
        if (order.getType() == OrderType.MARKET) return contraOrder.getPrice();
        if (contraOrder.getType() == OrderType.MARKET) return order.getPrice();
        return contraOrder.getPrice(); // Maker price
    }
}
