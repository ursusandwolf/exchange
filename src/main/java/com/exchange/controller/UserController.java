package com.exchange.controller;

import com.exchange.dto.PortfolioResponse;
import com.exchange.model.User;
import com.exchange.model.Wallet;
import com.exchange.repository.UserRepository;
import com.exchange.service.AccountService;
import com.exchange.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AccountService accountService;
    private final WalletService walletService;

    @GetMapping("/portfolio")
    public PortfolioResponse getPortfolio() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = accountService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
        
        Wallet wallet = walletService.getWallet(user.getId());
        
        return new PortfolioResponse(
            user.getId(),
            wallet.getBalances(),
            wallet.getReserved()
        );
    }

    @GetMapping("/history")
    public List<com.exchange.model.TransactionRecord> getHistory() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = accountService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
        
        return walletService.getHistory(user.getId());
    }
}
