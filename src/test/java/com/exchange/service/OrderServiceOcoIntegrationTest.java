package com.exchange.service;

import com.exchange.dto.OcoOrderRequest;
import com.exchange.dto.OrderRequest;
import com.exchange.enums.OrderStatus;
import com.exchange.enums.OrderType;
import com.exchange.enums.Side;
import com.exchange.model.User;
import com.exchange.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceOcoIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MatchingManager matchingManager;

    @Test
    void shouldActivateOneLegAndCancelSiblingForOcoSellOrder() {
        User seller = accountService.registerUser("oco-seller-" + System.currentTimeMillis(), "password123");
        User buyer = accountService.registerUser("oco-buyer-" + System.currentTimeMillis(), "password123");
        accountService.seedDemoBalances(seller.getId());
        accountService.seedDemoBalances(buyer.getId());

        OcoOrderRequest ocoRequest = new OcoOrderRequest(
                "BTC",
                "USDT",
                Side.SELL,
                new BigDecimal("1"),
                new BigDecimal("45000"),
                new BigDecimal("35000"),
                new BigDecimal("34900")
        );

        var ocoResponse = orderService.submitOcoOrder(seller, ocoRequest);

        assertThat(ocoResponse.orderIds()).hasSize(2);
        assertThat(orderRepository.findByOcoGroupId(ocoResponse.ocoGroupId())).hasSize(2);
        assertThat(walletService.getBalance(seller.getId(), "BTC")).isEqualByComparingTo("0");

        OrderRequest buyRequest = new OrderRequest(
                "BTC",
                "USDT",
                Side.BUY,
                OrderType.LIMIT,
                new BigDecimal("1"),
                new BigDecimal("45000"),
                null,
                null
        );
        orderService.submitOrder(buyer, buyRequest);

        orderService.checkAndActivateTriggeredOrders("BTC/USDT", new BigDecimal("45000"));

        var groupOrders = orderRepository.findByOcoGroupId(ocoResponse.ocoGroupId());
        assertThat(groupOrders).hasSize(3);
        assertThat(groupOrders).anyMatch(order -> order.getStatus() == OrderStatus.CANCELLED);
        assertThat(groupOrders).anyMatch(order -> order.getType() == OrderType.LIMIT && order.getStatus() == OrderStatus.FILLED);
        assertThat(groupOrders).anyMatch(order -> order.getType() == OrderType.TAKE_PROFIT && order.getStatus() == OrderStatus.FILLED);
        assertThat(matchingManager.findTriggeredOrdersByGroupId(ocoResponse.ocoGroupId())).isEmpty();
        assertThat(walletService.getBalance(buyer.getId(), "BTC")).isEqualByComparingTo("2");
        assertThat(walletService.getBalance(seller.getId(), "BTC")).isEqualByComparingTo("0");
        assertThat(walletService.getBalance(seller.getId(), "USDT")).isGreaterThan(new BigDecimal("50000"));
    }
}
