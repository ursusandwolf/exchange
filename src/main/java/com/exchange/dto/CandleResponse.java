package com.exchange.dto;

import java.math.BigDecimal;

public record CandleResponse(
        String symbol,
        String interval,
        long openTime,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume
) {}
