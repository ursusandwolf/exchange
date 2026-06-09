package com.exchange.dto;

import com.exchange.enums.Side;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OcoOrderRequest(
    @NotBlank(message = "Base asset is required")
    String baseAsset,

    @NotBlank(message = "Quote asset is required")
    String quoteAsset,

    @NotNull(message = "Side is required")
    Side side,

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    BigDecimal quantity,

    @NotNull(message = "Take profit price is required")
    @Positive(message = "Take profit price must be positive")
    BigDecimal takeProfitPrice,

    @NotNull(message = "Stop loss trigger price is required")
    @Positive(message = "Stop loss trigger price must be positive")
    BigDecimal stopLossTriggerPrice,

    @Positive(message = "Stop loss price must be positive")
    BigDecimal stopLossPrice
) {}

