package com.exchange.config;

import com.exchange.enums.Side;
import com.exchange.model.User;
import com.exchange.service.ExchangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Инициализатор данных для демонстрации.
 * Создаёт тестовых пользователей и начальные ордера в стакане.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class MarketDataInitializer implements CommandLineRunner {

    private final ExchangeService exchangeService;

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing demo market data...");

        // 1. Создаем системных мейкеров для ликвидности
        User maker1 = exchangeService.registerUser("maker_low", "password123");
        User maker2 = exchangeService.registerUser("maker_high", "password123");

        // Пополняем им балансы (теперь через сервис с сохранением в БД)
        exchangeService.deposit(maker1.getId(), "BTC", new BigDecimal("10"));
        exchangeService.deposit(maker1.getId(), "USDT", new BigDecimal("1000000"));
        exchangeService.deposit(maker2.getId(), "BTC", new BigDecimal("10"));
        exchangeService.deposit(maker2.getId(), "USDT", new BigDecimal("1000000"));

        // 2. Наполняем стакан BTC/USDT вокруг цены 65000
        BigDecimal basePrice = new BigDecimal("65000");

        // Bids (Покупка)
        for (int i = 1; i <= 5; i++) {
            BigDecimal price = basePrice.subtract(new BigDecimal(i * 100));
            exchangeService.submitOrder(maker1, "BTC", "USDT", Side.BUY, new BigDecimal("0.5"), price, null);
        }

        // Asks (Продажа)
        for (int i = 1; i <= 5; i++) {
            BigDecimal price = basePrice.add(new BigDecimal(i * 100));
            exchangeService.submitOrder(maker2, "BTC", "USDT", Side.SELL, new BigDecimal("0.5"), price, null);
        }

        log.info("Demo market data initialized successfully.");
    }
}
