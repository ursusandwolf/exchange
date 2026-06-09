package com.exchange.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionRecordResponse(
    String id,
    String userId,
    String asset,
    BigDecimal amount,
    String type,
    String description,
    Instant timestamp
) {}
