package com.exchange.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Кошелёк пользователя.
 * Хранит балансы по нескольким активам, поддерживает резервирование средств под ордеры.
 */
@Entity
@Getter
@ToString
@NoArgsConstructor
public class Wallet {
    @Id
    private String ownerId;
    
    // Доступный баланс по каждому активу
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "wallet_balances", joinColumns = @JoinColumn(name = "wallet_id"))
    @MapKeyColumn(name = "asset_symbol")
    @Column(name = "balance")
    private Map<String, BigDecimal> balances = new ConcurrentHashMap<>();
    
    // Зарезервированные средства под открытые ордера
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "wallet_reserved", joinColumns = @JoinColumn(name = "wallet_id"))
    @MapKeyColumn(name = "asset_symbol")
    @Column(name = "amount")
    private Map<String, BigDecimal> reserved = new ConcurrentHashMap<>();

    @Version
    private Long version;

    public Wallet(String ownerId) {
        this.ownerId = ownerId;
    }

    /**
     * Зачисление средств на кошелёк.
     */
    public synchronized void credit(String assetSymbol, BigDecimal amount) {
        validateAmount(amount);
        balances.merge(assetSymbol, amount, BigDecimal::add);
    }

    /**
     * Списывание средств с кошелька (с доступного баланса).
     * @throws IllegalArgumentException если недостаточно средств
     */
    public synchronized void debit(String assetSymbol, BigDecimal amount) {
        validateAmount(amount);
        BigDecimal balance = balances.getOrDefault(assetSymbol, BigDecimal.ZERO);
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                "Недостаточно средств " + assetSymbol + ". Баланс: " + balance + ", требуется: " + amount);
        }
        balances.put(assetSymbol, balance.subtract(amount));
    }

    /**
     * Резервирование средств под ордер.
     * Средства переходят из доступного баланса в зарезервированные.
     * @throws IllegalArgumentException если недостаточно доступных средств
     */
    public synchronized void reserve(String assetSymbol, BigDecimal amount) {
        validateAmount(amount);
        BigDecimal balance = balances.getOrDefault(assetSymbol, BigDecimal.ZERO);
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                "Недостаточно доступных средств " + assetSymbol + " для резервирования. Баланс: " + balance + ", требуется: " + amount);
        }
        balances.put(assetSymbol, balance.subtract(amount));
        reserved.merge(assetSymbol, amount, BigDecimal::add);
    }

    /**
     * Освобождение зарезервированных средств (возврат в доступный баланс).
     * Используется при отмене ордера или после частичного исполнения.
     */
    public synchronized void unreserve(String assetSymbol, BigDecimal amount) {
        validateAmount(amount);
        BigDecimal reservedAmount = reserved.getOrDefault(assetSymbol, BigDecimal.ZERO);
        if (reservedAmount.compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                "Недостаточно зарезервированных средств " + assetSymbol + ". Зарезервировано: " + reservedAmount + ", требуется освободить: " + amount);
        }
        reserved.put(assetSymbol, reservedAmount.subtract(amount));
        balances.merge(assetSymbol, amount, BigDecimal::add);
    }

    /**
     * Списывание зарезервированных средств (после исполнения сделки).
     */
    public synchronized void deductReserved(String assetSymbol, BigDecimal amount) {
        validateAmount(amount);
        BigDecimal reservedAmount = reserved.getOrDefault(assetSymbol, BigDecimal.ZERO);
        if (reservedAmount.compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                "Недостаточно зарезервированных средств " + assetSymbol + " для списания. Зарезервировано: " + reservedAmount + ", требуется: " + amount);
        }
        reserved.put(assetSymbol, reservedAmount.subtract(amount));
    }

    /**
     * Получение доступного баланса по активу.
     */
    public BigDecimal getBalance(String assetSymbol) {
        return balances.getOrDefault(assetSymbol, BigDecimal.ZERO);
    }

    /**
     * Получение зарезервированной суммы по активу.
     */
    public BigDecimal getReserved(String assetSymbol) {
        return reserved.getOrDefault(assetSymbol, BigDecimal.ZERO);
    }

    /**
     * Получение общего баланса (доступный + зарезервированный).
     */
    public BigDecimal getTotalBalance(String assetSymbol) {
        return getBalance(assetSymbol).add(getReserved(assetSymbol));
    }

    /**
     * Возвращает множество всех активов, имеющих ненулевой баланс.
     */
    public Set<String> getAssets() {
        Set<String> assets = new java.util.HashSet<>(balances.keySet());
        assets.addAll(reserved.keySet());
        return Collections.unmodifiableSet(assets);
    }

    // TODO: Здесь можно добавить:
    // - Поддержку нескольких валют одновременно с конвертацией
    // - Аудит всех операций (история транзакций)
    // - Интеграцию с внешними платёжными системами (для persistency)
    // - Лимиты на вывод/пополнение
    // - Подписку на события изменения баланса (Observer pattern)

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной");
        }
    }
}
