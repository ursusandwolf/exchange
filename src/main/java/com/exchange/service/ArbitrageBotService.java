package com.exchange.service;

import com.exchange.dto.OrderRequest;
import com.exchange.engine.OrderBook;
import com.exchange.enums.OrderType;
import com.exchange.enums.Side;
import com.exchange.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArbitrageBotService {
    private final OrderService orderService;
    private final AccountService accountService;
    private final WalletService walletService;
    private final MatchingManager matchingManager;
    private final ExternalPriceOracleService oracleService;

    private static final String BOT_USERNAME = "arbitrage_bot";
    private static final BigDecimal THRESHOLD_PERCENT = new BigDecimal("0.005"); // 0.5% deviation
    @Value("${app.arbitrage.symbol:BTC/USDT}")
    private String symbol;
    private String botUserId;

    @jakarta.annotation.PostConstruct
    public void init() {
        User bot = accountService.findByUsername(BOT_USERNAME)
                .orElseGet(() -> {
                    User u = accountService.registerUser(BOT_USERNAME, "bot_password_123");
                    walletService.deposit(u.getId(), "USDT", new BigDecimal("1000000"));
                    walletService.deposit(u.getId(), "BTC", new BigDecimal("10"));
                    return u;
                });
        this.botUserId = bot.getId();
        log.info("Arbitrage bot initialized with user ID: {}", botUserId);
    }

    @Scheduled(fixedRate = 10000) // Check every 10 seconds
    public void runArbitrage() {
        BigDecimal externalPrice = oracleService.getExternalPrice(symbol);
        if (externalPrice == null) return;

        OrderBook orderBook = matchingManager.getOrderBookBySymbol(symbol);
        if (orderBook == null) return;

        BigDecimal bestBid = orderBook.getBestBid();
        BigDecimal bestAsk = orderBook.getBestAsk();

        // 1. Buy opportunity: Internal Ask < External Price
        if (bestAsk != null) {
            BigDecimal diff = externalPrice.subtract(bestAsk);
            BigDecimal diffPercent = diff.divide(bestAsk, 8, RoundingMode.HALF_UP);
            
            if (diffPercent.compareTo(THRESHOLD_PERCENT) > 0) {
                log.info("Arbitrage opportunity found: Internal price {} is lower than external {}. BUYING BTC.", bestAsk, externalPrice);
                executeTrade(Side.BUY, new BigDecimal("0.1"), bestAsk.add(BigDecimal.ONE));
            }
        }

        // 2. Sell opportunity: Internal Bid > External Price
        if (bestBid != null) {
            BigDecimal diff = bestBid.subtract(externalPrice);
            BigDecimal diffPercent = diff.divide(externalPrice, 8, RoundingMode.HALF_UP);

            if (diffPercent.compareTo(THRESHOLD_PERCENT) > 0) {
                log.info("Arbitrage opportunity found: Internal price {} is higher than external {}. SELLING BTC.", bestBid, externalPrice);
                executeTrade(Side.SELL, new BigDecimal("0.1"), bestBid.subtract(BigDecimal.ONE));
            }
        }
    }

    private void executeTrade(Side side, BigDecimal quantity, BigDecimal price) {
        try {
            User bot = accountService.findById(botUserId).orElseThrow();
            String[] parts = symbol.split("/", 2);
            if (parts.length != 2) {
                throw new IllegalStateException("Invalid arbitrage symbol: " + symbol);
            }
            OrderRequest request = new OrderRequest(parts[0], parts[1], side, OrderType.LIMIT, quantity, price, null, null);
            orderService.submitOrder(bot, request);
        } catch (Exception e) {
            log.error("Bot trade failed: {}", e.getMessage());
        }
    }
}
