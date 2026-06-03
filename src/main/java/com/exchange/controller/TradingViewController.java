package com.exchange.controller;

import com.exchange.dto.tradingview.TvConfigResponse;
import com.exchange.dto.tradingview.TvHistoryResponse;
import com.exchange.dto.tradingview.TvSymbolResponse;
import com.exchange.model.Candle;
import com.exchange.repository.CandleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for TradingView UDF (Universal Data Feed).
 */
@RestController
@RequestMapping("/api/tradingview")
@RequiredArgsConstructor
@Slf4j
public class TradingViewController {

    private final CandleRepository candleRepository;

    @GetMapping("/config")
    public TvConfigResponse getConfig() {
        return TvConfigResponse.builder().build();
    }

    @GetMapping("/time")
    public long getTime() {
        return Instant.now().getEpochSecond();
    }

    @GetMapping("/symbols")
    public TvSymbolResponse getSymbol(@RequestParam String symbol) {
        return TvSymbolResponse.builder()
                .name(symbol)
                .ticker(symbol)
                .description("Gemini Simulation: " + symbol)
                .priceScale(100)
                .build();
    }

    @GetMapping("/history")
    public TvHistoryResponse getHistory(
            @RequestParam String symbol,
            @RequestParam String resolution,
            @RequestParam long from,
            @RequestParam long to
    ) {
        log.debug("History request: symbol={}, resolution={}, from={}, to={}", symbol, resolution, from, to);

        // Map TradingView resolution to our intervals
        String interval = mapResolution(resolution);
        
        LocalDateTime startTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(from), ZoneOffset.UTC);
        LocalDateTime endTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(to), ZoneOffset.UTC);

        List<Candle> candles = candleRepository.findBySymbolAndIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                symbol, interval, startTime, endTime
        );

        if (candles.isEmpty()) {
            return TvHistoryResponse.noData();
        }

        List<Long> t = new ArrayList<>();
        List<BigDecimal> o = new ArrayList<>();
        List<BigDecimal> h = new ArrayList<>();
        List<BigDecimal> l = new ArrayList<>();
        List<BigDecimal> c = new ArrayList<>();
        List<BigDecimal> v = new ArrayList<>();

        for (Candle candle : candles) {
            t.add(candle.getOpenTime().toInstant(ZoneOffset.UTC).getEpochSecond());
            o.add(candle.getOpen().value());
            h.add(candle.getHigh().value());
            l.add(candle.getLow().value());
            c.add(candle.getClose().value());
            v.add(candle.getVolume().value());
        }

        return TvHistoryResponse.builder()
                .status("ok")
                .timestamps(t)
                .open(o)
                .high(h)
                .low(l)
                .close(c)
                .volume(v)
                .build();
    }

    private String mapResolution(String resolution) {
        if (resolution.equals("1")) return "1m";
        if (resolution.equals("5")) return "5m";
        if (resolution.equals("15")) return "15m";
        if (resolution.equals("30")) return "30m";
        if (resolution.equals("60")) return "1h";
        if (resolution.equals("D")) return "1d";
        return "1m"; // default
    }
}
