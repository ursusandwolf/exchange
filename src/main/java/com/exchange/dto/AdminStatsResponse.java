package com.exchange.dto;

import java.math.BigDecimal;
import java.util.Map;

public record AdminStatsResponse(
    long totalUsers,
    long totalOrders,
    long activeOrders,
    long adminUsers,
    Map<String, BigDecimal> exchangeProfit,
    boolean systemUserPresent
) {}
