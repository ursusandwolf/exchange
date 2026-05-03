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
@ToString
@NoArgsConstructor
public class User {
    @Id
    private String id;
    
    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "wallet_id", referencedColumnName = "ownerId")
    private Wallet wallet;

    public User(String username, String password) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.password = password;
        this.wallet = new Wallet(this.id);
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
