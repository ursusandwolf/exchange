package com.exchange.service;

import com.exchange.model.User;
import com.exchange.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletService walletService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void changePassword_updatesPassword_whenCurrentPasswordMatches() {
        User user = new User("alice", passwordEncoder.encode("oldpass"));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountService accountService = new AccountService(userRepository, passwordEncoder, walletService);

        accountService.changePassword("user-1", "oldpass", "newpass123");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        org.assertj.core.api.Assertions.assertThat(passwordEncoder.matches("newpass123", captor.getValue().getPassword()))
                .isTrue();
    }

    @Test
    void changePassword_rejectsWhenCurrentPasswordIsWrong() {
        User user = new User("alice", passwordEncoder.encode("oldpass"));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        AccountService accountService = new AccountService(userRepository, passwordEncoder, walletService);

        assertThatThrownBy(() -> accountService.changePassword("user-1", "wrongpass", "newpass123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Текущий пароль неверный");
    }
}
