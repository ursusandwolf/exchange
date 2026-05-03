package com.exchange.dto;

import com.exchange.enums.Side;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record OrderRequest(
    @NotBlank(message = "Base asset is required")
    String baseAsset,

    @NotBlank(message = "Quote asset is required")
    String quoteAsset,

    @NotNull(message = "Side (BUY/SELL) is required")
    Side side,

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    BigDecimal quantity,

    @Positive(message = "Price must be positive")
    BigDecimal price
) {}
