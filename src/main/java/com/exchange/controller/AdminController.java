package com.exchange.controller;

import com.exchange.model.User;
import com.exchange.service.AccountService;
import com.exchange.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AccountService accountService;
    private final WalletService walletService;
    private static final String SYSTEM_FEE_USER = "SYSTEM_FEES";

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        User feeUser = accountService.findByUsername(SYSTEM_FEE_USER).orElse(null);
        Map<String, BigDecimal> fees = (feeUser != null) 
                ? feeUser.getWallet().getBalances() 
                : Map.of();
        
        return Map.of(
            "exchangeProfit", fees,
            "systemUserPresent", feeUser != null
        );
    }
}
