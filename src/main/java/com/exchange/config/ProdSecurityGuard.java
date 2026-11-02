package com.exchange.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fail-fast when production is started with demo secrets or H2 console enabled.
 */
@Component
@Profile("prod")
public class ProdSecurityGuard {

    private static final String DEV_JWT_PREFIX = "dev-only-";

    public ProdSecurityGuard(
            @Value("${app.security.jwt.secret}") String jwtSecret,
            @Value("${app.admin.bootstrap.password}") String adminPassword,
            @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled
    ) {
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.startsWith(DEV_JWT_PREFIX)) {
            throw new IllegalStateException("prod profile requires a non-dev JWT_SECRET");
        }
        if (adminPassword == null || adminPassword.isBlank() || "admin123".equals(adminPassword)) {
            throw new IllegalStateException("prod profile requires a strong ADMIN_PASSWORD");
        }
        if (h2ConsoleEnabled) {
            throw new IllegalStateException("prod profile forbids spring.h2.console.enabled=true");
        }
    }
}
