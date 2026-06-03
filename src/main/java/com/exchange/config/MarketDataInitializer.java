package com.exchange.config;

import com.exchange.dto.OrderRequest;
import com.exchange.enums.OrderType;
import com.exchange.enums.Side;
import com.exchange.model.User;
import com.exchange.service.AccountService;
import com.exchange.service.OrderService;
import com.exchange.service.WalletService;
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

    private final AccountService accountService;
    private final WalletService walletService;
    private final OrderService orderService;

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing demo market data...");

        // 1. Создаем системных мейкеров для ликвидности
        User maker1 = accountService.registerUser("maker_low", "password123");
        User maker2 = accountService.registerUser("maker_high", "password123");

        // Пополняем им балансы
        walletService.deposit(maker1.getId(), "BTC", new BigDecimal("10"));
        walletService.deposit(maker1.getId(), "USDT", new BigDecimal("1000000"));
        walletService.deposit(maker2.getId(), "BTC", new BigDecimal("10"));
        walletService.deposit(maker2.getId(), "USDT", new BigDecimal("1000000"));

        // 2. Наполняем стакан BTC/USDT вокруг цены 65000
        BigDecimal basePrice = new BigDecimal("65000");

        // Bids (Покупка)
        for (int i = 1; i <= 5; i++) {
            BigDecimal price = basePrice.subtract(new BigDecimal(i * 100));
            OrderRequest request = new OrderRequest("BTC", "USDT", Side.BUY, OrderType.LIMIT, new BigDecimal("0.5"), price, null, null);
            orderService.submitOrder(maker1, request);
        }

        // Asks (Продажа)
        for (int i = 1; i <= 5; i++) {
            BigDecimal price = basePrice.add(new BigDecimal(i * 100));
            OrderRequest request = new OrderRequest("BTC", "USDT", Side.SELL, OrderType.LIMIT, new BigDecimal("0.5"), price, null, null);
            orderService.submitOrder(maker2, request);
        }

        log.info("Demo market data initialized successfully.");
    }
}
