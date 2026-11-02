package com.exchange.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    @Test
    void usesRemoteAddrWhenForwardedHeadersAreNotTrusted() {
        ClientIpResolver resolver = new ClientIpResolver(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.5");
    }

    @Test
    void usesFirstForwardedIpWhenTrusted() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.9");
    }
}
