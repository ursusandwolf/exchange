package com.exchange.dto;

import com.exchange.enums.OrderStatus;
import com.exchange.enums.OrderType;
import com.exchange.enums.Side;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
    String id,
    String baseAsset,
    String quoteAsset,
    Side side,
    OrderType type,
    BigDecimal quantity,
    BigDecimal price,
    BigDecimal priceLimit,
    BigDecimal triggerPrice,
    String ocoGroupId,
    BigDecimal filledQuantity,
    OrderStatus status,
    Instant createdAt
) {}
