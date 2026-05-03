package com.exchange.model;

import com.exchange.enums.OrderStatus;
import com.exchange.enums.OrderType;
import com.exchange.enums.Side;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Заявка (ордер) на покупку или продажу актива.
 */
@Entity
@Table(name = "orders")
@Getter
@ToString(exclude = {"updatedAt"})
@NoArgsConstructor
public class Order {
    @Id
    private String id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private String baseAsset;      // Например, BTC
    
    @Column(nullable = false)
    private String quoteAsset;     // Например, USDT
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Side side;             // BUY или SELL
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type;        // LIMIT или MARKET
    
    @Column(precision = 24, scale = 8, nullable = false)
    private BigDecimal quantity;   // Количество базового актива
    
    @Column(precision = 24, scale = 8)
    private BigDecimal price;      // Цена за единицу (null для MARKET ордеров)
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    
    @Column(precision = 24, scale = 8, nullable = false)
    private BigDecimal filledQuantity;   // Сколько уже исполнено
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
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
}
