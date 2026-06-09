package com.exchange.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AdminDashboardResponse(
    long totalUsers,
    long totalOrders,
    long activeOrders,
    long adminUsers,
    Map<String, BigDecimal> exchangeProfit,
    boolean systemUserPresent,
    List<AdminUserResponse> users
) {}
