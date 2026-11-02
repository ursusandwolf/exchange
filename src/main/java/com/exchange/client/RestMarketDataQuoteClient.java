package com.exchange.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;

@Component
@Slf4j
public class RestMarketDataQuoteClient implements MarketDataQuoteClient {

    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    private final String baseUrl;
    private final String quotePath;

    public RestMarketDataQuoteClient(
            @Value("${app.market-data.base-url}") String baseUrl,
            @Value("${app.market-data.quote-path}") String quotePath,
            @Value("${app.market-data.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${app.market-data.read-timeout-ms:3000}") int readTimeoutMs
    ) {
        this.baseUrl = baseUrl;
        this.quotePath = quotePath;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.restTemplate = new RestTemplate(factory);

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(3)
                .build();
        this.circuitBreaker = CircuitBreaker.of("marketDataQuotes", config);
    }

    @Override
    public BigDecimal fetchPrice(String symbol) {
        try {
            return circuitBreaker.executeSupplier(() -> doFetch(symbol));
        } catch (Exception e) {
            log.warn("Market-data quote failed for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    private BigDecimal doFetch(String symbol) {
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
        private String price;
    }
}
