package com.exchange.model;

import com.alex.fin.core.domain.common.InstrumentId;
import com.alex.fin.core.domain.common.Price;
import com.alex.fin.core.domain.common.Quantity;
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
    
    @Column(nullable = false, length = 32)
    private InstrumentId baseAsset;      // Например, BTC

    @Column(nullable = false, length = 32)
    private InstrumentId quoteAsset;     // Например, USDT
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Side side;             // BUY или SELL
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type;        // LIMIT, MARKET, STOP_LOSS, TAKE_PROFIT
    
    @Column(precision = 24, scale = 8, nullable = false)
    private Quantity quantity;   // Количество базового актива
    
    @Column(length = 96)
    private Price price;      // Цена за единицу (null для MARKET ордеров)

    @Column(length = 96)
    private Price priceLimit; // Лимит проскальзывания для MARKET ордеров

    @Column(length = 96)
    private Price triggerPrice; // Цена активации для STOP_LOSS / TAKE_PROFIT

    @Column(length = 64)
    private String ocoGroupId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    
    @Column(precision = 24, scale = 8, nullable = false)
    private Quantity filledQuantity;   // Сколько уже исполнено

    @Column(precision = 24, scale = 8, nullable = false)
    private BigDecimal reservedAmount;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    private Order(String userId, InstrumentId baseAsset, InstrumentId quoteAsset, Side side, 
                  OrderType type, Quantity quantity, Price price, Price priceLimit, Price triggerPrice) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.baseAsset = baseAsset;
        this.quoteAsset = quoteAsset;
        this.side = side;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.priceLimit = priceLimit;
        this.triggerPrice = triggerPrice;
        this.status = OrderStatus.PENDING;
        this.filledQuantity = new Quantity(BigDecimal.ZERO);
        this.reservedAmount = BigDecimal.ZERO;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * Фабричный метод для создания LIMIT ордера.
     */
    public static Order limitOrder(String userId, InstrumentId baseAsset, InstrumentId quoteAsset, 
                                   Side side, Quantity quantity, Price price) {
        if (price == null || price.value().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Цена должна быть положительной для LIMIT ордера");
        }
        return new Order(userId, baseAsset, quoteAsset, side, OrderType.LIMIT, quantity, price, null, null);
    }

    /**
     * Фабричный метод для создания MARKET ордера.
     */
    public static Order marketOrder(String userId, InstrumentId baseAsset, InstrumentId quoteAsset, 
                                    Side side, Quantity quantity, Price priceLimit) {
        return new Order(userId, baseAsset, quoteAsset, side, OrderType.MARKET, quantity, null, priceLimit, null);
    }

    /**
     * Фабричный метод для создания STOP_LOSS / TAKE_PROFIT ордера.
     */
    public static Order triggerOrder(String userId, InstrumentId baseAsset, InstrumentId quoteAsset, 
                                    Side side, OrderType type, Quantity quantity, Price price, Price triggerPrice) {
        if (type != OrderType.STOP_LOSS && type != OrderType.TAKE_PROFIT) {
            throw new IllegalArgumentException("Trigger order type must be STOP_LOSS or TAKE_PROFIT");
        }
        if (triggerPrice == null || triggerPrice.value().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Trigger price is required");
        }
        return new Order(userId, baseAsset, quoteAsset, side, type, quantity, price, null, triggerPrice);
    }

    public void linkOcoGroup(String ocoGroupId) {
        this.ocoGroupId = ocoGroupId;
        this.updatedAt = Instant.now();
    }

    /**
     * Возвращает торговую пару в формате "BASE/QUOTE", например "BTC/USDT".
     */
    public String getSymbol() {
        return baseAsset.value() + "/" + quoteAsset.value();
    }

    /**
     * Обновляет количество исполненного ордера.
     */
    public synchronized void addFilledQuantity(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal newFilled = this.filledQuantity.value().add(amount);
        this.filledQuantity = new Quantity(newFilled);
        this.updatedAt = Instant.now();
        
        if (this.filledQuantity.value().compareTo(this.quantity.value()) >= 0) {
            this.status = OrderStatus.FILLED;
        } else if (this.filledQuantity.value().compareTo(BigDecimal.ZERO) > 0) {
            this.status = OrderStatus.PARTIALLY_FILLED;
        }
    }

    public synchronized void setReservedAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Reserved amount cannot be negative");
        }
        this.reservedAmount = amount;
        this.updatedAt = Instant.now();
    }

    public synchronized void consumeReservedAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal updated = this.reservedAmount.subtract(amount);
        if (updated.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Reserved amount underflow for order " + id);
        }
        this.reservedAmount = updated;
        this.updatedAt = Instant.now();
    }

    public synchronized void clearReservedAmount() {
        this.reservedAmount = BigDecimal.ZERO;
        this.updatedAt = Instant.now();
    }

    /**
     * Возвращает оставшееся количество для исполнения.
     */
    public BigDecimal getRemainingQuantity() {
        return this.quantity.value().subtract(this.filledQuantity.value());
    }

    /**
     * Проверяет, является ли ордер активным (может быть исполнен).
     */
    public boolean isActive() {
        return this.status == OrderStatus.PENDING || this.status == OrderStatus.PARTIALLY_FILLED;
    }

    /**
     * Отменяет ордер.
     * @throws IllegalStateException если ордер не активен
     */
    public synchronized void cancel() {
        if (!isActive()) {
            throw new IllegalStateException("Нельзя отменить неактивный ордер в статусе " + status);
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }
}
