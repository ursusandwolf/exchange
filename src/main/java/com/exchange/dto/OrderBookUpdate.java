package com.exchange.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class OrderBookUpdate {
    private String symbol;
    private List<PriceLevel> bids;
    private List<PriceLevel> asks;

    @Data
    @Builder
    public static class PriceLevel {
        private BigDecimal price;
        private BigDecimal quantity;
    }
}
