package com.exchange.service;

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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class ExchangeServiceIntegrationTest {

    @Autowired
    private ExchangeService exchangeService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldExecuteTradeAndPersistBalances() {
        // 1. Регистрация пользователей
        User alice = exchangeService.registerUser("Alice", "password123");
        User bob = exchangeService.registerUser("Bob", "password123");

        // 2. Пополнение балансов (не требуется, так как registerUser дает стартовый капитал)
        
        // 3. Bob выставляет SELL ордер
        exchangeService.submitOrder(
            bob, "BTC", "USDT", Side.SELL, 
            new BigDecimal("1"), new BigDecimal("45000"), null
        );

        // 4. Alice выставляет BUY ордер на ту же цену
        List<Trade> trades = exchangeService.submitOrder(
            alice, "BTC", "USDT", Side.BUY, 
            new BigDecimal("1"), new BigDecimal("45000"), null
        );

        // 5. Проверки (Assertions)
        assertThat(trades).hasSize(1);
        Trade trade = trades.get(0);
        assertThat(trade.getPrice()).isEqualByComparingTo("45000");
        assertThat(trade.getQuantity()).isEqualByComparingTo("1");
        
        // Проверяем комиссии
        // Alice (Taker): 45000 * 0.001 = 45 USDT
        // Bob (Maker): 45000 * 0.0005 = 22.5 USDT
        assertThat(trade.getBuyerFee()).isEqualByComparingTo("45");
        assertThat(trade.getSellerFee()).isEqualByComparingTo("22.5");

        // Перезагружаем пользователей, чтобы увидеть изменения из БД
        User aliceAfter = exchangeService.getUser(alice.getId());
        User bobAfter = exchangeService.getUser(bob.getId());

        // Проверка баланса Alice: было 50000 USDT, потратила 45000 + 45 USDT, получила 1 BTC (было 1 BTC -> стало 2 BTC)
        assertThat(aliceAfter.getWallet().getBalance("USDT")).isEqualByComparingTo("4955");
        assertThat(aliceAfter.getWallet().getBalance("BTC")).isEqualByComparingTo("2");

        // Проверка баланса Bob: было 50000 USDT, получил 45000 - 22.5 USDT, потратил 1 BTC (было 1 BTC -> стало 0 BTC)
        assertThat(bobAfter.getWallet().getBalance("USDT")).isEqualByComparingTo("94977.5");
        assertThat(bobAfter.getWallet().getBalance("BTC")).isEqualByComparingTo("0");
    }
}
