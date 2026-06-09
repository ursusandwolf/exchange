package com.exchange.client;

import java.math.BigDecimal;

public interface MarketDataQuoteClient {
    BigDecimal fetchPrice(String symbol);
}
