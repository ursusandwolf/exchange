package com.exchange.engine;

import com.alex.fin.core.domain.common.CurrencyCode;
import com.alex.fin.core.domain.common.InstrumentId;
import com.alex.fin.core.domain.common.Price;
import com.alex.fin.core.domain.common.Quantity;
import com.exchange.enums.OrderType;
import com.exchange.enums.Side;
import com.exchange.model.Order;
import com.exchange.model.Trade;
import com.exchange.service.FeeService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingEngineTest {

    private final MatchingEngine matchingEngine = new MatchingEngine(new FeeService());

    @Test
    void should_match_orders_in_fifo_order_at_same_price() {
        OrderBook orderBook = new OrderBook("BTC", "USDT");

        Order firstAsk = Order.limitOrder(
                "seller-1",
                new InstrumentId("BTC"),
                new InstrumentId("USDT"),
                Side.SELL,
                new Quantity(new BigDecimal("1")),
                new Price(new BigDecimal("45000"), new CurrencyCode("USDT"))
        );
        Order secondAsk = Order.limitOrder(
                "seller-2",
                new InstrumentId("BTC"),
                new InstrumentId("USDT"),
                Side.SELL,
                new Quantity(new BigDecimal("1")),
                new Price(new BigDecimal("45000"), new CurrencyCode("USDT"))
        );

        orderBook.addOrder(firstAsk);
        orderBook.addOrder(secondAsk);

        Order incomingBuy = Order.limitOrder(
                "buyer-1",
                new InstrumentId("BTC"),
                new InstrumentId("USDT"),
                Side.BUY,
                new Quantity(new BigDecimal("2")),
                new Price(new BigDecimal("46000"), new CurrencyCode("USDT"))
        );

        MatchResult result = matchingEngine.match(orderBook, incomingBuy);

        assertThat(result.getTrades()).hasSize(2);
        assertThat(result.getTrades().get(0).getMakerOrderId()).isEqualTo(firstAsk.getId());
        assertThat(result.getTrades().get(1).getMakerOrderId()).isEqualTo(secondAsk.getId());
        assertThat(result.getRemainingOrder()).isNull();
    }

    @Test
    void should_skip_inactive_contra_order_and_continue_matching() {
        OrderBook orderBook = new OrderBook("BTC", "USDT");

        Order staleAsk = Order.limitOrder(
                "seller-1",
                new InstrumentId("BTC"),
                new InstrumentId("USDT"),
                Side.SELL,
                new Quantity(new BigDecimal("1")),
                new Price(new BigDecimal("45000"), new CurrencyCode("USDT"))
        );
        staleAsk.cancel();

        Order activeAsk = Order.limitOrder(
                "seller-2",
                new InstrumentId("BTC"),
                new InstrumentId("USDT"),
                Side.SELL,
                new Quantity(new BigDecimal("1")),
                new Price(new BigDecimal("45000"), new CurrencyCode("USDT"))
        );

        orderBook.addOrder(staleAsk);
        orderBook.addOrder(activeAsk);

        Order incomingBuy = Order.limitOrder(
                "buyer-1",
                new InstrumentId("BTC"),
                new InstrumentId("USDT"),
                Side.BUY,
                new Quantity(new BigDecimal("1")),
                new Price(new BigDecimal("46000"), new CurrencyCode("USDT"))
        );

        MatchResult result = matchingEngine.match(orderBook, incomingBuy);

        assertThat(result.getTrades()).hasSize(1);
        assertThat(result.getTrades().get(0).getMakerOrderId()).isEqualTo(activeAsk.getId());
        assertThat(result.getOrdersToRemove()).contains(staleAsk, activeAsk);
    }

    @Test
    void should_stop_market_matching_when_price_limit_is_reached() {
        OrderBook orderBook = new OrderBook("BTC", "USDT");

        Order cheapAsk = Order.limitOrder(
                "seller-1",
                new InstrumentId("BTC"),
                new InstrumentId("USDT"),
                Side.SELL,
                new Quantity(new BigDecimal("1")),
                new Price(new BigDecimal("44000"), new CurrencyCode("USDT"))
        );
        Order expensiveAsk = Order.limitOrder(
                "seller-2",
                new InstrumentId("BTC"),
                new InstrumentId("USDT"),
                Side.SELL,
                new Quantity(new BigDecimal("1")),
                new Price(new BigDecimal("46000"), new CurrencyCode("USDT"))
        );

        orderBook.addOrder(cheapAsk);
        orderBook.addOrder(expensiveAsk);

        Order marketBuy = Order.marketOrder(
                "buyer-1",
                new InstrumentId("BTC"),
                new InstrumentId("USDT"),
                Side.BUY,
                new Quantity(new BigDecimal("2")),
                new Price(new BigDecimal("44500"), new CurrencyCode("USDT"))
        );

        MatchResult result = matchingEngine.match(orderBook, marketBuy);

        assertThat(result.getTrades()).hasSize(1);
        Trade trade = result.getTrades().get(0);
        assertThat(trade.getMakerOrderId()).isEqualTo(cheapAsk.getId());
        assertThat(result.getRemainingOrder()).isEqualTo(marketBuy);
        assertThat(result.getOrdersToRemove()).contains(cheapAsk);
    }

    @Test
    void should_return_remaining_order_when_partial_fill_occurs() {
        OrderBook orderBook = new OrderBook("BTC", "USDT");

        Order ask = Order.limitOrder(
                "seller-1",
                new InstrumentId("BTC"),
                new InstrumentId("USDT"),
                Side.SELL,
                new Quantity(new BigDecimal("1")),
                new Price(new BigDecimal("45000"), new CurrencyCode("USDT"))
        );
        orderBook.addOrder(ask);

        Order incomingBuy = Order.limitOrder(
                "buyer-1",
                new InstrumentId("BTC"),
                new InstrumentId("USDT"),
                Side.BUY,
                new Quantity(new BigDecimal("2")),
                new Price(new BigDecimal("46000"), new CurrencyCode("USDT"))
        );

        MatchResult result = matchingEngine.match(orderBook, incomingBuy);

        assertThat(result.getTrades()).hasSize(1);
        assertThat(result.getTrades().get(0).getQuantity().value()).isEqualByComparingTo("1");
        assertThat(result.getRemainingOrder()).isSameAs(incomingBuy);
        assertThat(result.getOrdersToRemove()).containsExactly(ask);
    }
}
