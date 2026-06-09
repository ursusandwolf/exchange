package com.exchange.controller;

import com.exchange.dto.AuthUser;
import com.exchange.dto.ChangePasswordRequest;
import com.exchange.dto.PortfolioResponse;
import com.exchange.dto.TransactionRecordResponse;
import com.exchange.mapper.TransactionRecordMapper;
import com.exchange.mapper.UserMapper;
import com.exchange.service.AccountService;
import com.exchange.service.WalletService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AccountService accountService;
    private final WalletService walletService;
    private final UserMapper userMapper;
    private final TransactionRecordMapper transactionRecordMapper;

    @GetMapping("/portfolio")
    public PortfolioResponse getPortfolio(@AuthenticationPrincipal AuthUser currentUser) {
        requireAuthenticatedUser(currentUser);
        return userMapper.toPortfolioResponse(accountService.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found")));
    }

    @GetMapping("/history")
    public List<TransactionRecordResponse> getHistory(@AuthenticationPrincipal AuthUser currentUser) {
        requireAuthenticatedUser(currentUser);
        return walletService.getHistory(currentUser.getId()).stream()
                .map(transactionRecordMapper::toResponse)
                .toList();
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AuthUser currentUser,
            @RequestBody @Valid ChangePasswordRequest request
    ) {
        requireAuthenticatedUser(currentUser);
        accountService.changePassword(currentUser.getId(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private void requireAuthenticatedUser(AuthUser currentUser) {
        if (currentUser == null) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated user is required");
        }
    }
}
