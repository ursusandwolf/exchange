package com.exchange.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProdSecurityGuardTest {

    @Test
    void rejectsDevJwtSecret() {
        assertThatThrownBy(() -> new ProdSecurityGuard(
                "dev-only-secret-key-with-sufficient-length-1234567890",
                "StrongPass!234",
                false
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void rejectsDefaultAdminPassword() {
        assertThatThrownBy(() -> new ProdSecurityGuard(
                "prod-secret-key-with-sufficient-length-1234567890",
                "admin123",
                false
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD");
    }

    @Test
    void rejectsEnabledH2Console() {
        assertThatThrownBy(() -> new ProdSecurityGuard(
                "prod-secret-key-with-sufficient-length-1234567890",
                "StrongPass!234",
                true
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("h2.console");
    }

    @Test
    void acceptsStrongProdConfig() {
        assertThatCode(() -> new ProdSecurityGuard(
                "prod-secret-key-with-sufficient-length-1234567890",
                "StrongPass!234",
                false
        )).doesNotThrowAnyException();
    }
}
