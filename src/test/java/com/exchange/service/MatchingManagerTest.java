package com.exchange.service;

import com.alex.fin.core.domain.common.CurrencyCode;
import com.alex.fin.core.domain.common.InstrumentId;
import com.alex.fin.core.domain.common.Price;
import com.alex.fin.core.domain.common.Quantity;
import com.exchange.enums.OrderType;
import com.exchange.enums.Side;
import com.exchange.model.Order;
import com.exchange.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MatchingManagerTest {

    @Mock
    private OrderRepository orderRepository;

    @Test
    void should_activate_stop_loss_sell_when_price_drops_to_trigger() {
        MatchingManager matchingManager = new MatchingManager(orderRepository);

        Order stopLossSell = Order.triggerOrder(
                "user-1",
                new InstrumentId("BTC"),
                new InstrumentId("USDT"),
                Side.SELL,
                OrderType.STOP_LOSS,
                new Quantity(new BigDecimal("1")),
                null,
                new Price(new BigDecimal("45000"), new CurrencyCode("USDT"))
        );

        matchingManager.addTriggeredOrder(stopLossSell);

        var activated = matchingManager.getAndRemoveTriggeredOrders("BTC/USDT", new BigDecimal("44999"));

        assertThat(activated).containsExactly(stopLossSell);
    }

    @Test
    void should_keep_take_profit_order_queued_when_price_has_not_reached_trigger() {
        MatchingManager matchingManager = new MatchingManager(orderRepository);

        Order takeProfitSell = Order.triggerOrder(
                "user-1",
                new InstrumentId("BTC"),
                new InstrumentId("USDT"),
                Side.SELL,
                OrderType.TAKE_PROFIT,
                new Quantity(new BigDecimal("1")),
                null,
                new Price(new BigDecimal("55000"), new CurrencyCode("USDT"))
        );

        matchingManager.addTriggeredOrder(takeProfitSell);

        var activated = matchingManager.getAndRemoveTriggeredOrders("BTC/USDT", new BigDecimal("54000"));

        assertThat(activated).isEmpty();
        assertThat(matchingManager.getAndRemoveTriggeredOrders("BTC/USDT", new BigDecimal("56000")))
                .containsExactly(takeProfitSell);
    }

    @Test
    void should_activate_stop_loss_buy_when_price_rises_to_trigger() {
        MatchingManager matchingManager = new MatchingManager(orderRepository);

        Order stopLossBuy = Order.triggerOrder(
                "user-1",
                new InstrumentId("BTC"),
                new InstrumentId("USDT"),
                Side.BUY,
                OrderType.STOP_LOSS,
                new Quantity(new BigDecimal("1")),
                null,
                new Price(new BigDecimal("51000"), new CurrencyCode("USDT"))
        );

        matchingManager.addTriggeredOrder(stopLossBuy);

        assertThat(matchingManager.getAndRemoveTriggeredOrders("BTC/USDT", new BigDecimal("50999"))).isEmpty();
        assertThat(matchingManager.getAndRemoveTriggeredOrders("BTC/USDT", new BigDecimal("51000")))
                .containsExactly(stopLossBuy);
    }

    @Test
    void should_activate_take_profit_buy_when_price_falls_to_trigger() {
        MatchingManager matchingManager = new MatchingManager(orderRepository);

        Order takeProfitBuy = Order.triggerOrder(
                "user-1",
                new InstrumentId("BTC"),
                new InstrumentId("USDT"),
                Side.BUY,
                OrderType.TAKE_PROFIT,
                new Quantity(new BigDecimal("1")),
                null,
                new Price(new BigDecimal("48000"), new CurrencyCode("USDT"))
        );

        matchingManager.addTriggeredOrder(takeProfitBuy);

        assertThat(matchingManager.getAndRemoveTriggeredOrders("BTC/USDT", new BigDecimal("48001"))).isEmpty();
        assertThat(matchingManager.getAndRemoveTriggeredOrders("BTC/USDT", new BigDecimal("48000")))
                .containsExactly(takeProfitBuy);
    }
}
