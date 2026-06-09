package com.exchange.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthUserTest {

    @Test
    void regularUserShouldHaveUserRole() {
        AuthUser user = new AuthUser("user-1", "alice", "secret", false, 0);

        assertThat(user.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void adminShouldHaveBothUserAndAdminRoles() {
        AuthUser admin = new AuthUser("user-1", "admin", "secret", true, 0);

        assertThat(admin.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER", "ROLE_ADMIN");
    }
}
