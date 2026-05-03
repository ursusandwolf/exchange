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

import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
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

        // 2. Пополнение балансов
        // Alice покупает 1 BTC за 45000. Комиссия Taker 0.1% = 45.
        // Нужно минимум 45045 USDT. Дадим с запасом.
        alice.getWallet().credit("USDT", new BigDecimal("50000"));
        bob.getWallet().credit("BTC", new BigDecimal("1"));
        
        userRepository.save(alice);
        userRepository.save(bob);
        userRepository.flush(); // Гарантируем сброс в БД

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

        // Проверка баланса Alice: потратила 45000 + 45 USDT, получила 1 BTC
        assertThat(aliceAfter.getWallet().getBalance("USDT")).isEqualByComparingTo("4955");
        assertThat(aliceAfter.getWallet().getBalance("BTC")).isEqualByComparingTo("1");

        // Проверка баланса Bob: получил 45000 - 22.5 USDT, потратил 1 BTC
        assertThat(bobAfter.getWallet().getBalance("USDT")).isEqualByComparingTo("44977.5");
        assertThat(bobAfter.getWallet().getBalance("BTC")).isEqualByComparingTo("0");
    }
}
