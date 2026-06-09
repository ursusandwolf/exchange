package com.exchange.dto;

import com.exchange.model.User;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

@Getter
@ToString(exclude = "password")
public class AuthUser implements UserDetails {
    private final String id;
    private final String username;
    private final String password;
    private final boolean admin;
    private final int tokenVersion;

    public AuthUser(String id, String username, String password, boolean admin, int tokenVersion) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.admin = admin;
        this.tokenVersion = tokenVersion;
    }

    public static AuthUser from(User user) {
        return new AuthUser(user.getId(), user.getUsername(), user.getPassword(), user.isAdmin(), user.getTokenVersion());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (admin) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN")
            );
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
