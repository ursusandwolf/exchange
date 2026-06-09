package com.exchange.service;

import com.exchange.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthRateLimiterTest {

    private final AuthRateLimiter limiter = new AuthRateLimiter(2, 60, 2, 60, 2, 60);

    @Test
    void shouldAllowRequestsWithinConfiguredWindow() {
        assertThatCode(() -> limiter.assertLoginAllowed("127.0.0.1", "alice"))
                .doesNotThrowAnyException();
        assertThatCode(() -> limiter.assertLoginAllowed("127.0.0.1", "alice"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectRequestsAfterLimitIsReached() {
        limiter.assertLoginAllowed("127.0.0.1", "alice");
        limiter.assertLoginAllowed("127.0.0.1", "alice");

        assertThatThrownBy(() -> limiter.assertLoginAllowed("127.0.0.1", "alice"))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("Too many login attempts");
    }

    @Test
    void shouldClearLoginStateAfterSuccessfulAuthentication() {
        limiter.assertLoginAllowed("127.0.0.1", "alice");
        limiter.clearLogin("127.0.0.1", "alice");

        assertThatCode(() -> limiter.assertLoginAllowed("127.0.0.1", "alice"))
                .doesNotThrowAnyException();
    }
}
