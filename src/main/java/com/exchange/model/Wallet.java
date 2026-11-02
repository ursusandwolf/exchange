package com.exchange.model;

import com.alex.fin.core.domain.common.Money;
import com.alex.fin.core.domain.common.ValidationUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Map;
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
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "wallet_balances", joinColumns = @JoinColumn(name = "wallet_id"))
    @MapKeyColumn(name = "asset_symbol")
    @Column(name = "balance")
    private Map<String, BigDecimal> balances = new ConcurrentHashMap<>();
    
    // Зарезервированные средства под открытые ордера
    @ElementCollection(fetch = FetchType.LAZY)
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
    public void credit(String assetSymbol, BigDecimal amount) {
        ValidationUtils.validatePositive(amount, "amount");
        String asset = ValidationUtils.normalize(assetSymbol, "asset");
        balances.merge(asset, amount, BigDecimal::add);
    }

    /** Typed credit via fin-core {@link Money}; storage remains BigDecimal maps. */
    public void credit(Money money) {
        credit(money.currency().value(), money.amount());
    }

    /**
     * Резервирование средств под ордер.
     */
    public void reserve(String assetSymbol, BigDecimal amount) {
        ValidationUtils.validatePositive(amount, "amount");
        String asset = ValidationUtils.normalize(assetSymbol, "asset");
        BigDecimal balance = balances.getOrDefault(asset, BigDecimal.ZERO);
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                "Недостаточно доступных средств " + asset + " для резервирования. Баланс: " + balance + ", требуется: " + amount);
        }
        balances.put(asset, balance.subtract(amount));
        reserved.merge(asset, amount, BigDecimal::add);
    }

    /**
     * Освобождение зарезервированных средств.
     */
    public void unreserve(String assetSymbol, BigDecimal amount) {
        ValidationUtils.validatePositive(amount, "amount");
        String asset = ValidationUtils.normalize(assetSymbol, "asset");
        BigDecimal reservedAmount = reserved.getOrDefault(asset, BigDecimal.ZERO);
        if (reservedAmount.compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                "Недостаточно зарезервированных средств " + asset + ". Зарезервировано: " + reservedAmount + ", требуется освободить: " + amount);
        }
        reserved.put(asset, reservedAmount.subtract(amount));
        balances.merge(asset, amount, BigDecimal::add);
    }

    /**
     * Списывание зарезервированных средств.
     */
    public void deductReserved(String assetSymbol, BigDecimal amount) {
        ValidationUtils.validatePositive(amount, "amount");
        String asset = ValidationUtils.normalize(assetSymbol, "asset");
        BigDecimal reservedAmount = reserved.getOrDefault(asset, BigDecimal.ZERO);
        if (reservedAmount.compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                "Недостаточно зарезервированных средств " + asset + " для списания. Зарезервировано: " + reservedAmount + ", требуется: " + amount);
        }
        reserved.put(asset, reservedAmount.subtract(amount));
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
}
