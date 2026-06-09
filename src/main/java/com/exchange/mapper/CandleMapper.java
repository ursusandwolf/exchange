package com.exchange.mapper;

import com.exchange.dto.CandleResponse;
import com.exchange.dto.tradingview.TvHistoryResponse;
import com.exchange.model.Candle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class CandleMapper {

    public CandleResponse toResponse(Candle candle) {
        return new CandleResponse(
                candle.getSymbol(),
                candle.getInterval(),
                candle.getOpenTime().toEpochSecond(ZoneOffset.UTC),
                candle.getOpen().value(),
                candle.getHigh().value(),
                candle.getLow().value(),
                candle.getClose().value(),
                candle.getVolume().value()
        );
    }

    public List<CandleResponse> toResponseList(List<Candle> candles) {
        return candles.stream().map(this::toResponse).toList();
    }

    public TvHistoryResponse toHistoryResponse(List<Candle> candles) {
        if (candles.isEmpty()) {
            return TvHistoryResponse.noData();
        }

        List<Long> timestamps = candles.stream()
                .map(candle -> candle.getOpenTime().toEpochSecond(ZoneOffset.UTC))
                .toList();
        List<BigDecimal> open = candles.stream().map(candle -> candle.getOpen().value()).toList();
        List<BigDecimal> high = candles.stream().map(candle -> candle.getHigh().value()).toList();
        List<BigDecimal> low = candles.stream().map(candle -> candle.getLow().value()).toList();
        List<BigDecimal> close = candles.stream().map(candle -> candle.getClose().value()).toList();
        List<BigDecimal> volume = candles.stream().map(candle -> candle.getVolume().value()).toList();

        return TvHistoryResponse.builder()
                .status("ok")
                .timestamps(timestamps)
                .open(open)
                .high(high)
                .low(low)
                .close(close)
                .volume(volume)
                .build();
    }
}
