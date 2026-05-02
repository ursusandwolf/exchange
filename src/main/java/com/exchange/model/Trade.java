package com.exchange.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Результат исполнения сделки между двумя ордерами.
 * DTO для передачи информации о совершённой сделке.
 */
public class Trade {
    private final String id;
    private final String symbol;           // Торговая пара, например BTC/USDT
    private final Order buyOrder;          // Ордер на покупку
    private final Order sellOrder;         // Ордер на продажу
    private final BigDecimal price;        // Цена исполнения
    private final BigDecimal quantity;     // Количество исполненного актива
    private final BigDecimal totalAmount;  // Общая сумма (price * quantity)
    private final Instant timestamp;
    
    // Комиссии (пока не используются,预留 для будущего расширения)
    private final BigDecimal buyerFee;
    private final BigDecimal sellerFee;

    public Trade(Order buyOrder, Order sellOrder, BigDecimal price, BigDecimal quantity) {
        this.id = UUID.randomUUID().toString();
        this.symbol = buyOrder.getSymbol();
        this.buyOrder = buyOrder;
        this.sellOrder = sellOrder;
        this.price = price;
        this.quantity = quantity;
        this.totalAmount = price.multiply(quantity);
        this.timestamp = Instant.now();
        // TODO: Здесь можно добавить расчёт комиссий на основе maker/taker ставок
        this.buyerFee = BigDecimal.ZERO;
        this.sellerFee = BigDecimal.ZERO;
    }

    // Геттеры
    public String getId() { return id; }
    public String getSymbol() { return symbol; }
    public Order getBuyOrder() { return buyOrder; }
    public Order getSellOrder() { return sellOrder; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getTimestamp() { return timestamp; }
    public BigDecimal getBuyerFee() { return buyerFee; }
    public BigDecimal getSellerFee() { return sellerFee; }

    /**
     * Возвращает ID покупателя.
     */
    public String getBuyerId() {
        return buyOrder.getUserId();
    }

    /**
     * Возвращает ID продавца.
     */
    public String getSellerId() {
        return sellOrder.getUserId();
    }

    @Override
    public String toString() {
        return "Trade{" +
                "id='" + id + '\'' +
                ", symbol='" + symbol + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", total=" + totalAmount +
                ", buyer=" + getBuyerId() +
                ", seller=" + getSellerId() +
                '}';
    }
}
