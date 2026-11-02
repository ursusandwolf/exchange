package com.exchange.service;

import com.exchange.model.TransactionRecord;
import com.exchange.model.User;
import com.exchange.model.Wallet;
import com.exchange.repository.TransactionRecordRepository;
import com.exchange.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {
    private final UserRepository userRepository;
    private final TransactionRecordRepository transactionRepository;

    @Transactional
    public void deposit(String userId, String asset, BigDecimal amount) {
        mutate(userId, asset, amount, amount, "DEPOSIT", "External deposit", Wallet::credit);
    }

    @Transactional
    public void credit(String userId, String asset, BigDecimal amount, String description) {
        mutate(userId, asset, amount, amount, "CREDIT", description, Wallet::credit);
    }

    @Transactional
    public void debitReserved(String userId, String asset, BigDecimal amount, String description) {
        mutate(userId, asset, amount, amount.negate(), "DEBIT_RESERVED", description, Wallet::deductReserved);
    }

    @Transactional
    public void reserve(String userId, String asset, BigDecimal amount, String description) {
        mutate(userId, asset, amount, amount.negate(), "RESERVE", description, Wallet::reserve);
    }

    @Transactional
    public void unreserve(String userId, String asset, BigDecimal amount, String description) {
        mutate(userId, asset, amount, amount, "UNRESERVE", description, Wallet::unreserve);
    }

    private void mutate(String userId,
                        String asset,
                        BigDecimal walletAmount,
                        BigDecimal auditAmount,
                        String type,
                        String description,
                        WalletMutation mutation) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + userId));
        mutation.apply(user.getWallet(), asset, walletAmount);
        recordTransaction(userId, asset, auditAmount, type, description);
        userRepository.save(user);
    }

    private void recordTransaction(String userId, String asset, BigDecimal amount, String type, String description) {
        transactionRepository.save(TransactionRecord.builder()
                .userId(userId)
                .asset(asset)
                .amount(amount)
                .type(type)
                .description(description)
                .timestamp(Instant.now())
                .build());
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(String userId, String asset) {
        return getWallet(userId).getBalance(asset);
    }

    public List<TransactionRecord> getHistory(String userId) {
        return transactionRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    public Wallet getWallet(String userId) {
        return userRepository.findById(userId)
                .map(User::getWallet)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @FunctionalInterface
    private interface WalletMutation {
        void apply(Wallet wallet, String asset, BigDecimal amount);
    }
}
