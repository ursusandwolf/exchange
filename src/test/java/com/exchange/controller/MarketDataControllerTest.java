package com.exchange.controller;

import com.alex.fin.core.domain.common.CurrencyCode;
import com.alex.fin.core.domain.common.Price;
import com.alex.fin.core.domain.common.Quantity;
import com.exchange.model.Candle;
import com.exchange.mapper.CandleMapper;
import com.exchange.repository.CandleRepository;
import com.exchange.service.ExternalPriceOracleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MarketDataControllerTest {

    @Mock
    private CandleRepository candleRepository;

    @Mock
    private ExternalPriceOracleService oracleService;

    private final CandleMapper candleMapper = new CandleMapper();

    @Test
    void should_return_candle_dto_list_instead_of_entity() {
        Candle candle = Candle.builder()
                .symbol("BTC/USDT")
                .interval("1m")
                .openTime(LocalDateTime.of(2026, 6, 8, 12, 30))
                .open(new Price(new BigDecimal("50000"), new CurrencyCode("USDT")))
                .high(new Price(new BigDecimal("51000"), new CurrencyCode("USDT")))
                .low(new Price(new BigDecimal("49000"), new CurrencyCode("USDT")))
                .close(new Price(new BigDecimal("50500"), new CurrencyCode("USDT")))
                .volume(new Quantity(new BigDecimal("10")))
                .build();

        given(candleRepository.findBySymbolAndIntervalOrderByOpenTimeDesc("BTC/USDT", "1m"))
                .willReturn(List.of(candle));

        MarketDataController marketDataController = new MarketDataController(candleRepository, candleMapper, oracleService);

        var response = marketDataController.getCandles("BTC_USDT", "1m");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).symbol()).isEqualTo("BTC/USDT");
        assertThat(response.get(0).openTime()).isEqualTo(LocalDateTime.of(2026, 6, 8, 12, 30).toEpochSecond(java.time.ZoneOffset.UTC));
        assertThat(response.get(0).open()).isEqualTo(new BigDecimal("50000"));
        assertThat(response.get(0).volume()).isEqualTo(new BigDecimal("10"));
    }
}
