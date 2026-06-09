package com.exchange.service;

import com.alex.fin.core.domain.common.CurrencyCode;
import com.alex.fin.core.domain.common.Price;
import com.alex.fin.core.domain.common.Quantity;
import com.exchange.dto.CandleResponse;
import com.exchange.dto.TradeResponse;
import com.exchange.mapper.CandleMapper;
import com.exchange.mapper.TradeMapper;
import com.exchange.model.Candle;
import com.exchange.model.Order;
import com.exchange.model.Trade;
import com.exchange.repository.CandleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketDataServiceTest {

    private static final TimeZone ORIGINAL_TIME_ZONE = TimeZone.getDefault();

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private CandleRepository candleRepository;

    @Mock
    private TradeMapper tradeMapper;

    private final CandleMapper candleMapper = new CandleMapper();

    @AfterEach
    void restoreTimezone() {
        TimeZone.setDefault(ORIGINAL_TIME_ZONE);
    }

    @Test
    void should_bucket_candle_using_utc_even_when_server_timezone_differs() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));

        MarketDataService service = new MarketDataService(messagingTemplate, candleRepository, candleMapper, tradeMapper);

        Order buyOrder = org.mockito.Mockito.mock(Order.class);
        Order sellOrder = org.mockito.Mockito.mock(Order.class);
        Trade trade = org.mockito.Mockito.mock(Trade.class);

        Instant tradeTime = Instant.parse("2026-06-08T12:34:56Z");
        TradeResponse tradeResponse = new TradeResponse(
                "trade-1",
                "buy-1",
                "sell-1",
                new BigDecimal("65000"),
                new BigDecimal("1"),
                new BigDecimal("65000"),
                tradeTime
        );

        Candle existing = Candle.builder()
                .symbol("BTC/USDT")
                .interval("1m")
                .openTime(LocalDateTime.of(2026, 6, 8, 12, 34))
                .open(new Price(new BigDecimal("65000"), new CurrencyCode("USDT")))
                .high(new Price(new BigDecimal("65000"), new CurrencyCode("USDT")))
                .low(new Price(new BigDecimal("65000"), new CurrencyCode("USDT")))
                .close(new Price(new BigDecimal("65000"), new CurrencyCode("USDT")))
                .volume(new Quantity(new BigDecimal("1")))
                .build();

        when(trade.getPrice()).thenReturn(new Price(new BigDecimal("65000"), new CurrencyCode("USDT")));
        when(trade.getQuantity()).thenReturn(new Quantity(new BigDecimal("1")));
        when(trade.getTimestamp()).thenReturn(tradeTime);
        when(trade.getBuyOrder()).thenReturn(buyOrder);
        when(buyOrder.getSymbol()).thenReturn("BTC/USDT");
        when(tradeMapper.toResponse(trade)).thenReturn(tradeResponse);
        when(candleRepository.findBySymbolAndIntervalAndOpenTime(
                eq("BTC/USDT"),
                eq("1m"),
                eq(LocalDateTime.of(2026, 6, 8, 12, 34))
        )).thenReturn(Optional.of(existing));
        when(candleRepository.save(any(Candle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.broadcastTrade(trade);

        verify(candleRepository).findBySymbolAndIntervalAndOpenTime(
                "BTC/USDT",
                "1m",
                LocalDateTime.of(2026, 6, 8, 12, 34)
        );

        ArgumentCaptor<Candle> captor = ArgumentCaptor.forClass(Candle.class);
        verify(candleRepository).save(captor.capture());
        assertThat(captor.getValue().getOpenTime()).isEqualTo(LocalDateTime.of(2026, 6, 8, 12, 34));
        verify(messagingTemplate).convertAndSend(eq("/topic/candles/BTC/USDT/1m"), any(CandleResponse.class));
    }
}
