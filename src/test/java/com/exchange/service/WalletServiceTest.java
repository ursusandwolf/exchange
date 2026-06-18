package com.exchange.service;

import com.exchange.model.TransactionRecord;
import com.exchange.model.User;
import com.exchange.model.Wallet;
import com.exchange.repository.TransactionRecordRepository;
import com.exchange.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRecordRepository transactionRepository;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(userRepository, transactionRepository);
    }

    @Test
    void deposit_shouldCreditWalletAndSaveUser() {
        User user = new User("user", "pass");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        walletService.deposit(user.getId(), "USD", new BigDecimal("100"));

        assertThat(user.getWallet().getBalance("USD")).isEqualTo(new BigDecimal("100"));
        verify(userRepository).save(user);
        verify(transactionRepository).save(any(TransactionRecord.class));
    }

    @Test
    void reserve_shouldReserveFundsAndRecordTransaction() {
        User user = new User("user", "pass");
        user.getWallet().credit("USD", new BigDecimal("100"));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        walletService.reserve(user.getId(), "USD", new BigDecimal("50"), "Reserve test");

        assertThat(user.getWallet().getBalance("USD")).isEqualTo(new BigDecimal("50"));
        assertThat(user.getWallet().getReserved("USD")).isEqualTo(new BigDecimal("50"));
        verify(userRepository).save(user);
        
        ArgumentCaptor<TransactionRecord> recordCaptor = ArgumentCaptor.forClass(TransactionRecord.class);
        verify(transactionRepository).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getType()).isEqualTo("RESERVE");
        assertThat(recordCaptor.getValue().getAmount()).isEqualTo(new BigDecimal("-50"));
    }
}
