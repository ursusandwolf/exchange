package com.exchange.dto;

import java.math.BigDecimal;
import java.util.Map;

public record AdminUserResponse(
    String userId,
    String username,
    boolean admin,
    Map<String, BigDecimal> balances,
    Map<String, BigDecimal> reserved
) {}
