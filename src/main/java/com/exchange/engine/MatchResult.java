package com.exchange.engine;

import com.exchange.model.Order;
import com.exchange.model.Trade;
import lombok.Value;

import java.util.List;

/**
 * Результат работы Matching Engine.
 * Содержит информацию о совершенных сделках и изменениях, которые нужно применить к стакану.
 */
@Value
public class MatchResult {
    List<Trade> trades;
    List<Order> ordersToRemove;
    Order remainingOrder; // Ордер, который нужно добавить в стакан (если остался объем)
}
