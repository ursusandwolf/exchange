package com.exchange.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalPriceOracleService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, BigDecimal> externalPrices = new ConcurrentHashMap<>();

    // URL для получения цены BTC/USDT с Binance (публичный API)
    private static final String BINANCE_TICKER_URL = "https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT";

    @Scheduled(fixedRate = 5000) // Обновляем каждые 5 секунд
    public void fetchPrices() {
        try {
            BinancePriceResponse response = restTemplate.getForObject(BINANCE_TICKER_URL, BinancePriceResponse.class);
            if (response != null && response.getPrice() != null) {
                BigDecimal price = new BigDecimal(response.getPrice());
                externalPrices.put("BTC/USDT", price);
                log.debug("Fetched external price for BTC/USDT: {}", price);
            }
        } catch (Exception e) {
            log.error("Failed to fetch external price: {}", e.getMessage());
        }
    }

    public BigDecimal getExternalPrice(String symbol) {
        return externalPrices.get(symbol);
    }

    @Data
    private static class BinancePriceResponse {
        private String symbol;
        private String price;
    }
}
