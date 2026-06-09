package com.exchange.controller;

import com.alex.fin.core.domain.common.CurrencyCode;
import com.alex.fin.core.domain.common.Price;
import com.alex.fin.core.domain.common.Quantity;
import com.exchange.dto.tradingview.TvHistoryResponse;
import com.exchange.mapper.CandleMapper;
import com.exchange.model.Candle;
import com.exchange.repository.CandleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TradingViewControllerTest {

    @Mock
    private CandleRepository candleRepository;

    @Test
    void should_return_history_when_candles_exist() {
        // Given
        String symbol = "BTC/USDT";
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Candle candle = Candle.builder()
                .symbol(symbol)
                .interval("1m")
                .openTime(now)
                .open(new Price(new BigDecimal("50000"), new CurrencyCode("USDT")))
                .high(new Price(new BigDecimal("51000"), new CurrencyCode("USDT")))
                .low(new Price(new BigDecimal("49000"), new CurrencyCode("USDT")))
                .close(new Price(new BigDecimal("50500"), new CurrencyCode("USDT")))
                .volume(new Quantity(new BigDecimal("10")))
                .build();

        given(candleRepository.findBySymbolAndIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                eq(symbol), eq("1m"), any(), any()))
                .willReturn(List.of(candle));

        // When
        TradingViewController tradingViewController = new TradingViewController(candleRepository, new CandleMapper());
        TvHistoryResponse response = tradingViewController.getHistory(
                symbol, "1", now.toEpochSecond(ZoneOffset.UTC) - 60, now.toEpochSecond(ZoneOffset.UTC) + 60
        );

        // Then
        assertThat(response.getStatus()).isEqualTo("ok");
        assertThat(response.getTimestamps()).hasSize(1);
        assertThat(response.getOpen()).containsExactly(new BigDecimal("50000"));
        assertThat(response.getClose()).containsExactly(new BigDecimal("50500"));
    }

    @Test
    void should_return_no_data_when_no_candles() {
        // Given
        given(candleRepository.findBySymbolAndIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                anyString(), anyString(), any(), any()))
                .willReturn(List.of());

        // When
        TradingViewController tradingViewController = new TradingViewController(candleRepository, new CandleMapper());
        TvHistoryResponse response = tradingViewController.getHistory("BTC/USDT", "1", 0, 100);

        // Then
        assertThat(response.getStatus()).isEqualTo("no_data");
    }
}
