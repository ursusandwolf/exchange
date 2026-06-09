package com.exchange.service;

import com.exchange.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthRateLimiter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private final int loginMaxAttempts;
    private final Duration loginWindow;
    private final int registerMaxAttempts;
    private final Duration registerWindow;
    private final int passwordResetMaxAttempts;
    private final Duration passwordResetWindow;

    public AuthRateLimiter(
            @Value("${app.security.rate-limit.login.max-attempts:5}") int loginMaxAttempts,
            @Value("${app.security.rate-limit.login.window-seconds:900}") long loginWindowSeconds,
            @Value("${app.security.rate-limit.register.max-attempts:10}") int registerMaxAttempts,
            @Value("${app.security.rate-limit.register.window-seconds:900}") long registerWindowSeconds,
            @Value("${app.security.rate-limit.password-reset.max-attempts:5}") int passwordResetMaxAttempts,
            @Value("${app.security.rate-limit.password-reset.window-seconds:900}") long passwordResetWindowSeconds
    ) {
        this.loginMaxAttempts = loginMaxAttempts;
        this.loginWindow = Duration.ofSeconds(loginWindowSeconds);
        this.registerMaxAttempts = registerMaxAttempts;
        this.registerWindow = Duration.ofSeconds(registerWindowSeconds);
        this.passwordResetMaxAttempts = passwordResetMaxAttempts;
        this.passwordResetWindow = Duration.ofSeconds(passwordResetWindowSeconds);
    }

    public void assertLoginAllowed(String clientKey, String username) {
        assertAllowed("login:" + normalized(clientKey) + ":" + normalized(username), loginMaxAttempts, loginWindow, "Too many login attempts. Please try again later.");
    }

    public void assertRegisterAllowed(String clientKey, String username, String email) {
        assertAllowed("register:" + normalized(clientKey) + ":" + normalized(username) + ":" + normalized(email), registerMaxAttempts, registerWindow, "Too many registration attempts. Please try again later.");
    }

    public void assertPasswordResetAllowed(String clientKey, String email) {
        assertAllowed("password-reset:" + normalized(clientKey) + ":" + normalized(email), passwordResetMaxAttempts, passwordResetWindow, "Too many password reset requests. Please try again later.");
    }

    public void clearLogin(String clientKey, String username) {
        windows.remove("login:" + normalized(clientKey) + ":" + normalized(username));
    }

    private void assertAllowed(String key, int maxAttempts, Duration window, String message) {
        Instant now = Instant.now();
        windows.compute(key, (ignored, current) -> {
            if (current == null || current.isExpired(now, window)) {
                return new Window(now, 1);
            }
            if (current.attempts >= maxAttempts) {
                throw new TooManyRequestsException(message);
            }
            return current.increment();
        });
    }

    private String normalized(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private record Window(Instant startedAt, int attempts) {
        boolean isExpired(Instant now, Duration window) {
            return startedAt.plus(window).isBefore(now);
        }

        Window increment() {
            return new Window(startedAt, attempts + 1);
        }
    }
}
