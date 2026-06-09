package com.exchange.service;

import com.exchange.model.PasswordResetToken;
import com.exchange.model.User;
import com.exchange.repository.PasswordResetTokenRepository;
import com.exchange.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordResetMailer mailer;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void requestPasswordReset_savesTokenAndSendsEmail_whenUserExists() {
        User user = new User("alice", passwordEncoder.encode("oldpass"), false, "alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PasswordResetService service = new PasswordResetService(userRepository, tokenRepository, passwordEncoder, mailer);
        service.requestPasswordReset("alice@example.com");

        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(mailer).sendResetEmail(org.mockito.ArgumentMatchers.eq("alice@example.com"), any());
    }

    @Test
    void resetPassword_updatesPasswordAndInvalidatesTokens() throws Exception {
        User user = new User("alice", passwordEncoder.encode("oldpass"), false, "alice@example.com");
        String token = "reset-token";
        String tokenHash = sha256(token);
        PasswordResetToken resetToken = new PasswordResetToken(user.getId(), tokenHash, Instant.now().plusSeconds(600));

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(tokenRepository.findByUserIdAndUsedAtIsNull(user.getId())).thenReturn(List.of(resetToken));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PasswordResetService service = new PasswordResetService(userRepository, tokenRepository, passwordEncoder, mailer);
        service.resetPassword(token, "newpass123");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(passwordEncoder.matches("newpass123", userCaptor.getValue().getPassword())).isTrue();
        assertThat(userCaptor.getValue().getTokenVersion()).isEqualTo(1);
        assertThat(resetToken.isUsed()).isTrue();
    }

    private static String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashed) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
