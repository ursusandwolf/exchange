package com.exchange.controller;

import com.exchange.dto.CandleResponse;
import com.exchange.mapper.CandleMapper;
import com.exchange.repository.CandleRepository;
import com.exchange.service.ExternalPriceOracleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketDataController {

    private final CandleRepository candleRepository;
    private final CandleMapper candleMapper;
    private final ExternalPriceOracleService oracleService;

    @GetMapping("/oracle/{symbol}")
    public BigDecimal getOraclePrice(@PathVariable String symbol) {
        String formattedSymbol = symbol.replace("_", "/");
        return oracleService.getExternalPrice(formattedSymbol);
    }

    @GetMapping("/candles/{symbol}")
    public List<CandleResponse> getCandles(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1m") String interval) {
        String formattedSymbol = symbol.replace("_", "/");
        return candleMapper.toResponseList(
                candleRepository.findBySymbolAndIntervalOrderByOpenTimeDesc(formattedSymbol, interval)
        );
    }
}
