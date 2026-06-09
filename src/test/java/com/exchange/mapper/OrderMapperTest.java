package com.exchange.mapper;

import com.alex.fin.core.domain.common.CurrencyCode;
import com.alex.fin.core.domain.common.InstrumentId;
import com.alex.fin.core.domain.common.Price;
import com.alex.fin.core.domain.common.Quantity;
import com.exchange.enums.OrderStatus;
import com.exchange.enums.OrderType;
import com.exchange.enums.Side;
import com.exchange.model.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    private final OrderMapper orderMapper = new OrderMapper();

    @Test
    void should_include_trigger_and_limit_prices_in_response() {
        Order order = Order.triggerOrder(
                "user-1",
                new InstrumentId("BTC"),
                new InstrumentId("USDT"),
                Side.SELL,
                OrderType.TAKE_PROFIT,
                new Quantity(new BigDecimal("1")),
                new Price(new BigDecimal("56000"), new CurrencyCode("USDT")),
                new Price(new BigDecimal("55000"), new CurrencyCode("USDT"))
        );

        var response = orderMapper.toResponse(order);

        assertThat(response.price()).isEqualByComparingTo("56000");
        assertThat(response.triggerPrice()).isEqualByComparingTo("55000");
        assertThat(response.priceLimit()).isNull();
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.createdAt()).isBeforeOrEqualTo(Instant.now());
    }
}
