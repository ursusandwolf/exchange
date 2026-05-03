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

@SpringBootTest
class ExchangeServiceIntegrationTest {

    @Autowired
    private ExchangeService exchangeService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldExecuteTradeAndPersistBalances() {
        // 1. Регистрация пользователей
        User alice = exchangeService.registerUser("Alice");
        User bob = exchangeService.registerUser("Bob");

        // 2. Пополнение балансов
        alice.getWallet().credit("USDT", new BigDecimal("50000"));
        bob.getWallet().credit("BTC", new BigDecimal("1"));
        
        // ВАЖНО: сохраняем состояние после пополнения, 
        // так как submitOrder будет загружать пользователей из БД
        userRepository.save(alice);
        userRepository.save(bob);

        // 3. Bob выставляет SELL ордер
        exchangeService.submitOrder(
            bob, "BTC", "USDT", Side.SELL, 
            new BigDecimal("1"), new BigDecimal("45000")
        );

        // 4. Alice выставляет BUY ордер на ту же цену
        List<Trade> trades = exchangeService.submitOrder(
            alice, "BTC", "USDT", Side.BUY, 
            new BigDecimal("1"), new BigDecimal("45000")
        );

        // 5. Проверки (Assertions)
        assertThat(trades).hasSize(1);
        Trade trade = trades.get(0);
        assertThat(trade.getPrice()).isEqualByComparingTo("45000");
        assertThat(trade.getQuantity()).isEqualByComparingTo("1");

        // Перезагружаем пользователей, чтобы увидеть изменения из БД
        User aliceAfter = exchangeService.getUser(alice.getId());
        User bobAfter = exchangeService.getUser(bob.getId());

        // Проверка баланса Alice: потратила 45000 USDT, получила 1 BTC
        assertThat(aliceAfter.getWallet().getBalance("USDT")).isEqualByComparingTo("5000");
        assertThat(aliceAfter.getWallet().getBalance("BTC")).isEqualByComparingTo("1");

        // Проверка баланса Bob: получил 45000 USDT, потратил 1 BTC
        assertThat(bobAfter.getWallet().getBalance("USDT")).isEqualByComparingTo("45000");
        assertThat(bobAfter.getWallet().getBalance("BTC")).isEqualByComparingTo("0");
    }
}
