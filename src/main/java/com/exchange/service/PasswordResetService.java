package com.exchange.service;

import com.exchange.model.PasswordResetToken;
import com.exchange.model.User;
import com.exchange.repository.PasswordResetTokenRepository;
import com.exchange.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int TOKEN_BYTES = 32;
    private static final long TOKEN_TTL_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailer mailer;

    @Transactional
    public void requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();
        tokenRepository.findByUserIdAndUsedAtIsNull(user.getId()).forEach(existing -> {
            existing.markUsed();
            tokenRepository.save(existing);
        });

        String token = generateToken();
        String tokenHash = hash(token);
        PasswordResetToken resetToken = new PasswordResetToken(user.getId(), tokenHash, Instant.now().plusSeconds(TOKEN_TTL_MINUTES * 60));
        tokenRepository.save(resetToken);

        mailer.sendResetEmail(email, token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        PasswordResetToken resetToken = tokenRepository.findByTokenHash(hash(token))
                .orElseThrow(() -> new IllegalArgumentException("Reset token is invalid"));

        if (resetToken.isUsed() || resetToken.isExpired()) {
            throw new IllegalArgumentException("Reset token is invalid or expired");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.changePassword(passwordEncoder.encode(newPassword));
        user.incrementTokenVersion();
        userRepository.save(user);

        tokenRepository.findByUserIdAndUsedAtIsNull(user.getId()).forEach(existing -> {
            existing.markUsed();
            tokenRepository.save(existing);
        });
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash reset token", e);
        }
    }
}
