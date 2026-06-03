package com.exchange.service;

import com.exchange.model.User;
import com.exchange.model.Wallet;
import com.exchange.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import com.exchange.model.TransactionRecord;
import com.exchange.repository.TransactionRecordRepository;
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + userId));
        user.getWallet().credit(asset, amount);
        
        recordTransaction(userId, asset, amount, "DEPOSIT", "External deposit");
        
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

    @Transactional
    public void credit(String userId, String asset, BigDecimal amount, String description) {
        User user = userRepository.findById(userId).orElseThrow();
        user.getWallet().credit(asset, amount);
        recordTransaction(userId, asset, amount, "CREDIT", description);
        userRepository.save(user);
    }

    @Transactional
    public void debitReserved(String userId, String asset, BigDecimal amount, String description) {
        User user = userRepository.findById(userId).orElseThrow();
        user.getWallet().deductReserved(asset, amount);
        recordTransaction(userId, asset, amount.negate(), "DEBIT_RESERVED", description);
        userRepository.save(user);
    }
    
    @Transactional
    public void reserve(String userId, String asset, BigDecimal amount, String description) {
        User user = userRepository.findById(userId).orElseThrow();
        user.getWallet().reserve(asset, amount);
        recordTransaction(userId, asset, amount.negate(), "RESERVE", description);
        userRepository.save(user);
    }

    @Transactional
    public void unreserve(String userId, String asset, BigDecimal amount, String description) {
        User user = userRepository.findById(userId).orElseThrow();
        user.getWallet().unreserve(asset, amount);
        recordTransaction(userId, asset, amount, "UNRESERVE", description);
        userRepository.save(user);
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
}
