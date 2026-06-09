package com.exchange.service;

import com.exchange.dto.OrderRequest;
import com.exchange.enums.OrderType;
import com.exchange.enums.Side;
import com.exchange.model.User;
import com.exchange.model.Trade;
import com.exchange.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.context.ActiveProfiles;

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
}
