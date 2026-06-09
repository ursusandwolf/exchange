package com.exchange.service;

import com.exchange.client.MarketDataQuoteClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalPriceOracleService {

    private final MarketDataQuoteClient marketDataQuoteClient;
    private final Map<String, BigDecimal> externalPrices = new ConcurrentHashMap<>();

    @Value("${app.market-data.symbols:BTC/USDT}")
    private String symbolsConfig;

    @Scheduled(fixedRate = 5000) // Обновляем каждые 5 секунд
    public void fetchPrices() {
        for (String symbol : configuredSymbols()) {
            try {
                BigDecimal price = marketDataQuoteClient.fetchPrice(symbol);
                if (price != null) {
                    externalPrices.put(symbol, price);
                    log.debug("Fetched external price for {}", symbol);
                }
            } catch (Exception e) {
                log.error("Failed to fetch external price for {}: {}", symbol, e.getMessage());
            }
        }
    }

    public BigDecimal getExternalPrice(String symbol) {
        return externalPrices.get(symbol);
    }

    private java.util.List<String> configuredSymbols() {
        return Arrays.stream(symbolsConfig.split(","))
                .map(String::trim)
                .filter(symbol -> !symbol.isBlank())
                .toList();
    }
}
