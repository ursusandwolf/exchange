package com.exchange.service;

import com.exchange.dto.OrderRequest;
import com.exchange.enums.OrderStatus;
import com.exchange.enums.OrderType;
import com.exchange.enums.Side;
import com.exchange.model.Order;
import com.exchange.model.Trade;
import com.exchange.model.User;
import com.exchange.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletService walletService;

    @Test
    void shouldExecuteTradeAndPersistBalances() {
        // 1. Регистрация пользователей
        User alice = accountService.registerUser("Alice_" + System.currentTimeMillis(), "password123");
        User bob = accountService.registerUser("Bob_" + System.currentTimeMillis(), "password123");
        accountService.seedDemoBalances(alice.getId());
        accountService.seedDemoBalances(bob.getId());

        // 2. Bob выставляет SELL ордер
        OrderRequest sellRequest = new OrderRequest("BTC", "USDT", Side.SELL, OrderType.LIMIT, new BigDecimal("1"), new BigDecimal("45000"), null, null);
        orderService.submitOrder(bob, sellRequest);

        // 3. Alice выставляет BUY ордер на ту же цену
        OrderRequest buyRequest = new OrderRequest("BTC", "USDT", Side.BUY, OrderType.LIMIT, new BigDecimal("1"), new BigDecimal("45000"), null, null);
        List<Trade> trades = orderService.submitOrder(alice, buyRequest);

        // 4. Проверки
        assertThat(trades).hasSize(1);
        Trade trade = trades.get(0);
        assertThat(trade.getPrice().value()).isEqualByComparingTo("45000");
        
        // Проверка баланса Alice: было 50000 USDT, потратила 45000 + 45 fee = 45045. Осталось 4955.
        assertThat(walletService.getBalance(alice.getId(), "USDT")).isEqualByComparingTo("4955");
        
        // Проверка системного кошелька с комиссиями
        User feeCollector = userRepository.findByUsername(OrderService.SYSTEM_FEE_USER).orElseThrow();
        assertThat(walletService.getBalance(feeCollector.getId(), "USDT")).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void partialFill_shouldKeepReserveEqualToRemainingSellQty() {
        User seller = accountService.registerUser("Seller_" + System.currentTimeMillis(), "password123");
        User buyer = accountService.registerUser("Buyer_" + System.currentTimeMillis(), "password123");
        walletService.deposit(seller.getId(), "ETH", new BigDecimal("2"));
        walletService.deposit(buyer.getId(), "USDT", new BigDecimal("100000"));

        orderService.submitOrder(seller, new OrderRequest(
                "ETH", "USDT", Side.SELL, OrderType.LIMIT,
                new BigDecimal("2"), new BigDecimal("3000"), null, null));

        orderService.submitOrder(buyer, new OrderRequest(
                "ETH", "USDT", Side.BUY, OrderType.LIMIT,
                new BigDecimal("1"), new BigDecimal("3000"), null, null));

        assertThat(walletService.getReserved(seller.getId(), "ETH"))
                .isEqualByComparingTo("1");
    }

    @Test
    void cancelAfterPartialFill_shouldReleaseRemainingReserve() {
        User seller = accountService.registerUser("CancelSeller_" + System.currentTimeMillis(), "password123");
        User buyer = accountService.registerUser("CancelBuyer_" + System.currentTimeMillis(), "password123");
        walletService.deposit(seller.getId(), "SOL", new BigDecimal("3"));
        walletService.deposit(buyer.getId(), "USDT", new BigDecimal("100000"));

        orderService.submitOrder(seller, new OrderRequest(
                "SOL", "USDT", Side.SELL, OrderType.LIMIT,
                new BigDecimal("3"), new BigDecimal("100"), null, null));

        orderService.submitOrder(buyer, new OrderRequest(
                "SOL", "USDT", Side.BUY, OrderType.LIMIT,
                new BigDecimal("1"), new BigDecimal("100"), null, null));

        Order openSell = orderService.getUserOrders(seller.getId()).stream()
                .filter(order -> order.getStatus() == OrderStatus.PARTIALLY_FILLED)
                .findFirst()
                .orElseThrow();

        assertThat(walletService.getReserved(seller.getId(), "SOL")).isEqualByComparingTo("2");

        boolean cancelled = orderService.cancelOrder(seller.getId(), openSell.getId());

        assertThat(cancelled).isTrue();
        assertThat(walletService.getReserved(seller.getId(), "SOL")).isEqualByComparingTo("0");
        assertThat(walletService.getBalance(seller.getId(), "SOL")).isEqualByComparingTo("2");
        assertThat(orderService.getUserOrders(seller.getId()).get(0).getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
