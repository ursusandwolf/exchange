package com.exchange.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Пользователь биржи.
 * Содержит базовую информацию и ссылку на кошелёк.
 */
@Entity
@Table(name = "users")
@Getter
@ToString(exclude = "password")
@NoArgsConstructor
public class User {
    @Id
    private String id;
    
    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean admin;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "wallet_id", referencedColumnName = "ownerId")
    private Wallet wallet;

    @Version
    private Long version;

    @Column(nullable = false)
    private int tokenVersion;

    public User(String username, String password) {
        this(username, password, false, username + "@exchange.local");
    }

    public User(String username, String password, boolean admin) {
        this(username, password, admin, username + "@exchange.local");
    }

    public User(String username, String password, boolean admin, String email) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.email = email;
        this.password = password;
        this.admin = admin;
        this.wallet = new Wallet(this.id);
        this.tokenVersion = 0;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public void incrementTokenVersion() {
        this.tokenVersion++;
    }

    /**
     * Пополнение баланса актива (для учебных целей, без реальных денег).
     * 
     * // TODO: Здесь можно добавить лимиты на пополнение, верификацию пользователя,
     * // аудит операций в лог событий.
     */
    public void deposit(String assetSymbol, BigDecimal amount) {
        wallet.credit(assetSymbol, amount);
    }
}
