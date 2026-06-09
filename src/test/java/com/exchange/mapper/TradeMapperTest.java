package com.exchange.mapper;

import com.alex.fin.core.domain.common.CurrencyCode;
import com.alex.fin.core.domain.common.InstrumentId;
import com.alex.fin.core.domain.common.Price;
import com.alex.fin.core.domain.common.Quantity;
import com.exchange.dto.TradeResponse;
import com.exchange.model.Order;
import com.exchange.model.Trade;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class TradeMapperTest {

    private final TradeMapper tradeMapper = new TradeMapper();

    @Test
    void should_preserve_instant_timestamp_without_timezone_conversion() {
        Order buyOrder = Mockito.mock(Order.class);
        Order sellOrder = Mockito.mock(Order.class);
        Trade trade = Mockito.mock(Trade.class);
        Instant timestamp = Instant.parse("2026-06-08T12:34:56Z");

        when(trade.getId()).thenReturn("trade-1");
        when(trade.getBuyOrder()).thenReturn(buyOrder);
        when(trade.getSellOrder()).thenReturn(sellOrder);
        when(buyOrder.getId()).thenReturn("buy-1");
        when(sellOrder.getId()).thenReturn("sell-1");
        when(trade.getPrice()).thenReturn(new Price(new BigDecimal("65000"), new CurrencyCode("USDT")));
        when(trade.getQuantity()).thenReturn(new Quantity(new BigDecimal("1")));
        when(trade.getTotalAmount()).thenReturn(new BigDecimal("65000"));
        when(trade.getTimestamp()).thenReturn(timestamp);

        TradeResponse response = tradeMapper.toResponse(trade);

        assertThat(response.timestamp()).isEqualTo(timestamp);
        assertThat(response.price()).isEqualByComparingTo("65000");
    }
}
