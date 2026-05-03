package com.exchange.controller;

import com.exchange.model.Candle;
import com.exchange.repository.CandleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.exchange.service.ExternalPriceOracleService;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketDataController {

    private final CandleRepository candleRepository;
    private final ExternalPriceOracleService oracleService;

    @GetMapping("/oracle/{symbol}")
    public BigDecimal getOraclePrice(@PathVariable String symbol) {
        String formattedSymbol = symbol.replace("_", "/");
        return oracleService.getExternalPrice(formattedSymbol);
    }

    @GetMapping("/candles/{symbol}")
    public List<Candle> getCandles(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1m") String interval) {
        // Заменяем _ на / в символе, если передано в формате BTC_USDT
        String formattedSymbol = symbol.replace("_", "/");
        return candleRepository.findBySymbolAndIntervalOrderByOpenTimeDesc(formattedSymbol, interval);
    }
}
