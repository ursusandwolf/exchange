package com.exchange.mapper;

import com.exchange.dto.AuthResponse;
import com.exchange.dto.PortfolioResponse;
import com.exchange.model.User;
import com.exchange.model.Wallet;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public AuthResponse toAuthResponse(User user, String token) {
        return new AuthResponse(token, user.getUsername(), user.getId(), user.isAdmin());
    }

    public PortfolioResponse toPortfolioResponse(User user) {
        Wallet wallet = user.getWallet();
        return new PortfolioResponse(user.getId(), wallet.getBalances(), wallet.getReserved());
    }
}
