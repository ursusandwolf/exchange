package com.exchange.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeResponse(
    String tradeId,
    String buyOrderId,
    String sellOrderId,
    BigDecimal price,
    BigDecimal quantity,
    BigDecimal totalAmount,
    LocalDateTime timestamp
) {}
