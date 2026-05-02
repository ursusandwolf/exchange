package com.exchange.model;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Пользователь биржи.
 * Содержит базовую информацию и ссылку на кошелёк.
 */
public class User {
    private final String id;
    private final String username;
    private final Wallet wallet;

    public User(String username) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.wallet = new Wallet(this.id);
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Wallet getWallet() {
        return wallet;
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

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", wallet=" + wallet +
                '}';
    }
}
