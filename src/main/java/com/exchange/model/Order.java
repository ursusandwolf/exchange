package com.exchange.model;

import com.exchange.enums.OrderStatus;
import com.exchange.enums.OrderType;
import com.exchange.enums.Side;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Заявка (ордер) на покупку или продажу актива.
 */
public class Order {
    private final String id;
    private final String userId;
    private final String baseAsset;      // Например, BTC
    private final String quoteAsset;     // Например, USDT
    private final Side side;             // BUY или SELL
    private final OrderType type;        // LIMIT или MARKET
    private final BigDecimal quantity;   // Количество базового актива
    private final BigDecimal price;      // Цена за единицу (null для MARKET ордеров)
    
    private OrderStatus status;
    private BigDecimal filledQuantity;   // Сколько уже исполнено
    private Instant createdAt;
    private Instant updatedAt;

    private Order(String userId, String baseAsset, String quoteAsset, Side side, 
                  OrderType type, BigDecimal quantity, BigDecimal price) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.baseAsset = baseAsset;
        this.quoteAsset = quoteAsset;
        this.side = side;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.status = OrderStatus.PENDING;
        this.filledQuantity = BigDecimal.ZERO;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * Фабричный метод для создания LIMIT ордера.
     */
    public static Order limitOrder(String userId, String baseAsset, String quoteAsset, 
                                   Side side, BigDecimal quantity, BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Цена должна быть положительной для LIMIT ордера");
        }
        return new Order(userId, baseAsset, quoteAsset, side, OrderType.LIMIT, quantity, price);
    }

    /**
     * Фабричный метод для создания MARKET ордера.
     */
    public static Order marketOrder(String userId, String baseAsset, String quoteAsset, 
                                    Side side, BigDecimal quantity) {
        return new Order(userId, baseAsset, quoteAsset, side, OrderType.MARKET, quantity, null);
    }

    /**
     * Возвращает торговую пару в формате "BASE/QUOTE", например "BTC/USDT".
     */
    public String getSymbol() {
        return baseAsset + "/" + quoteAsset;
    }

    /**
     * Обновляет количество исполненного ордера.
     */
    public synchronized void addFilledQuantity(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        this.filledQuantity = this.filledQuantity.add(amount);
        this.updatedAt = Instant.now();
        
        if (this.filledQuantity.compareTo(this.quantity) >= 0) {
            this.status = OrderStatus.FILLED;
        } else if (this.filledQuantity.compareTo(BigDecimal.ZERO) > 0) {
            this.status = OrderStatus.PARTIALLY_FILLED;
        }
    }

    /**
     * Возвращает оставшееся количество для исполнения.
     */
    public BigDecimal getRemainingQuantity() {
        return this.quantity.subtract(this.filledQuantity);
    }

    /**
     * Проверяет, является ли ордер активным (может быть исполнен).
     */
    public boolean isActive() {
        return this.status == OrderStatus.PENDING || this.status == OrderStatus.PARTIALLY_FILLED;
    }

    // Геттеры
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getBaseAsset() { return baseAsset; }
    public String getQuoteAsset() { return quoteAsset; }
    public Side getSide() { return side; }
    public OrderType getType() { return type; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getFilledQuantity() { return filledQuantity; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // TODO: Здесь можно добавить:
    // - TimeInForce (GTC, IOC, FOK) для управления временем жизни ордера
    // - Комиссии maker/taker
    // - Stop-loss и take-profit уровни
    // - Историю изменений статуса (аудит)
    // - Методы для persistency (сериализация в БД)

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", symbol='" + getSymbol() + '\'' +
                ", side=" + side +
                ", type=" + type +
                ", quantity=" + quantity +
                ", price=" + price +
                ", status=" + status +
                ", filled=" + filledQuantity +
                '}';
    }
}
