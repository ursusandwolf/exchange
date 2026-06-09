package com.exchange.service;

import com.exchange.engine.OrderBook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArbitrageBotServiceTest {

    @Mock
    private OrderService orderService;
    @Mock
    private AccountService accountService;
    @Mock
    private WalletService walletService;
    @Mock
    private MatchingManager matchingManager;
    @Mock
    private ExternalPriceOracleService oracleService;

    @Test
    void should_use_configured_symbol_instead_of_hardcoded_btc_usdt() {
        ArbitrageBotService botService = new ArbitrageBotService(orderService, accountService, walletService, matchingManager, oracleService);
        ReflectionTestUtils.setField(botService, "symbol", "ETH/USDT");

        when(oracleService.getExternalPrice("ETH/USDT")).thenReturn(new BigDecimal("3000"));
        when(matchingManager.getOrderBookBySymbol("ETH/USDT")).thenReturn(new OrderBook("ETH", "USDT"));

        botService.runArbitrage();

        verify(oracleService).getExternalPrice("ETH/USDT");
        verify(matchingManager).getOrderBookBySymbol("ETH/USDT");
    }
}
