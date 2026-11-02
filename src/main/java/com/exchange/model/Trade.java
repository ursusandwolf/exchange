package com.exchange.model;

import com.alex.fin.core.domain.common.Price;
import com.alex.fin.core.domain.common.Quantity;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Результат исполнения сделки между двумя ордерами.
 */
@Getter
@ToString(exclude = {"buyOrder", "sellOrder"})
public class Trade {
    private final String id;
    private final Order buyOrder;
    private final Order sellOrder;
    private final Price price;
    private final Quantity quantity;
    private final BigDecimal totalAmount;
    private final Instant timestamp;
    private final BigDecimal buyerFee;
    private final BigDecimal sellerFee;
    private final String makerOrderId;

    public Trade(Order buyOrder, Order sellOrder, Price price, Quantity quantity,
                 BigDecimal buyerFee, BigDecimal sellerFee, String makerOrderId) {
        this.id = UUID.randomUUID().toString();
        this.buyOrder = buyOrder;
        this.sellOrder = sellOrder;
        this.price = price;
        this.quantity = quantity;
        this.totalAmount = price.value().multiply(quantity.value());
        this.timestamp = Instant.now();
        this.buyerFee = buyerFee;
        this.sellerFee = sellerFee;
        this.makerOrderId = makerOrderId;
    }

    public String getBuyerId() {
        return buyOrder.getUserId();
    }

    public String getSellerId() {
        return sellOrder.getUserId();
    }
}
