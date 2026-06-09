package com.exchange.client;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Component
public class RestMarketDataQuoteClient implements MarketDataQuoteClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.market-data.base-url}")
    private String baseUrl;

    @Value("${app.market-data.quote-path}")
    private String quotePath;

    @Override
    public BigDecimal fetchPrice(String symbol) {
        QuoteResponse response = restTemplate.getForObject(buildQuoteUrl(symbol), QuoteResponse.class);
        if (response == null || response.getPrice() == null) {
            return null;
        }
        return new BigDecimal(response.getPrice());
    }

    private String buildQuoteUrl(String symbol) {
        String normalizedSymbol = symbol.replace("/", "").replace("_", "");
        return baseUrl + quotePath.replace("{symbol}", normalizedSymbol);
    }

    @Data
    private static class QuoteResponse {
        private String symbol;
        private String price;
    }
}
