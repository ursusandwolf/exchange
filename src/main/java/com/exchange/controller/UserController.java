package com.exchange.controller;

import com.exchange.dto.PortfolioResponse;
import com.exchange.model.User;
import com.exchange.model.Wallet;
import com.exchange.repository.UserRepository;
import com.exchange.service.ExchangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final ExchangeService exchangeService;
    private final UserRepository userRepository;

    @GetMapping("/portfolio")
    public PortfolioResponse getPortfolio() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
        
        Wallet wallet = exchangeService.getUserWallet(user.getId());
        
        return new PortfolioResponse(
            user.getId(),
            wallet.getBalances(),
            wallet.getReserved()
        );
    }
}
