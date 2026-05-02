package com.exchange;

import com.exchange.enums.Side;
import com.exchange.model.Order;
import com.exchange.model.Trade;
import com.exchange.model.User;
import com.exchange.service.ExchangeService;

import java.math.BigDecimal;
import java.util.List;

/**
 * Демо-приложение: пример сценария работы криптобиржи.
 * 
 * Сценарий:
 * 1. Регистрация двух пользователей (Alice и Bob)
 * 2. Пополнение их кошельков тестовыми активами
 * 3. Выставление SELL ордера (Bob продаёт BTC)
 * 4. Выставление BUY ордера (Alice покупает BTC)
 * 5. Исполнение сделки
 * 6. Проверка балансов после сделки
 */
public class ExchangeDemo {

    public static void main(String[] args) {
        System.out.println("=== Crypto Exchange Demo ===\n");

        // Создаём сервис биржи
        ExchangeService exchange = new ExchangeService();

        // === Шаг 1: Регистрация пользователей ===
        System.out.println("1. Регистрация пользователей...");
        User alice = exchange.registerUser("Alice");
        User bob = exchange.registerUser("Bob");
        System.out.println("   Alice: " + alice.getId());
        System.out.println("   Bob: " + bob.getId());
        System.out.println();

        // === Шаг 2: Пополнение кошельков ===
        System.out.println("2. Пополнение кошельков тестовыми активами...");
        
        // Alice получает 100,000 USDT для покупки BTC
        alice.deposit("USDT", new BigDecimal("100000"));
        System.out.println("   Alice баланс USDT: " + alice.getWallet().getBalance("USDT"));
        
        // Bob получает 5 BTC для продажи
        bob.deposit("BTC", new BigDecimal("5"));
        System.out.println("   Bob баланс BTC: " + bob.getWallet().getBalance("BTC"));
        System.out.println();

        // === Шаг 3: Bob выставляет SELL ордер (LIMIT) ===
        System.out.println("3. Bob выставляет LIMIT SELL ордер: 2 BTC по 45,000 USDT...");
        List<Trade> trades1 = exchange.submitOrder(
            bob,
            "BTC",           // base asset
            "USDT",          // quote asset
            Side.SELL,       // направление
            new BigDecimal("2"),      // количество
            new BigDecimal("45000")   // цена
        );
        System.out.println("   Ордер создан: " + (trades1.isEmpty() ? "ожидает исполнения" : "исполнен"));
        System.out.println("   Сделок на этом этапе: " + trades1.size());
        System.out.println("   Bob зарезервировано BTC: " + bob.getWallet().getReserved("BTC"));
        System.out.println("   Bob доступно BTC: " + bob.getWallet().getBalance("BTC"));
        System.out.println();

        // === Шаг 4: Alice выставляет BUY ордер (LIMIT) ===
        System.out.println("4. Alice выставляет LIMIT BUY ордер: 1 BTC по 45,000 USDT...");
        List<Trade> trades2 = exchange.submitOrder(
            alice,
            "BTC",
            "USDT",
            Side.BUY,
            new BigDecimal("1"),
            new BigDecimal("45000")
        );
        
        System.out.println("   Сделок исполнено: " + trades2.size());
        for (Trade trade : trades2) {
            System.out.println("   >>> Сделка: " + trade);
        }
        System.out.println();

        // === Шаг 5: Проверка балансов после сделки ===
        System.out.println("5. Балансы после сделки:");
        System.out.println("   Alice:");
        System.out.println("     - BTC: " + alice.getWallet().getBalance("BTC"));
        System.out.println("     - USDT: " + alice.getWallet().getBalance("USDT"));
        System.out.println("     - Зарезервировано USDT: " + alice.getWallet().getReserved("USDT"));
        
        System.out.println("   Bob:");
        System.out.println("     - BTC: " + bob.getWallet().getBalance("BTC"));
        System.out.println("     - USDT: " + bob.getWallet().getBalance("USDT"));
        System.out.println("     - Зарезервировано BTC: " + bob.getWallet().getReserved("BTC"));
        System.out.println();

        // === Шаг 6: Alice выставляет MARKET BUY ордер ===
        System.out.println("6. Alice выставляет MARKET BUY ордер: 0.5 BTC (по лучшей цене)...");
        List<Trade> trades3 = exchange.submitOrder(
            alice,
            "BTC",
            "USDT",
            Side.BUY,
            new BigDecimal("0.5"),
            null  // MARKET ордер
        );
        
        System.out.println("   Сделок исполнено: " + trades3.size());
        for (Trade trade : trades3) {
            System.out.println("   >>> Сделка: " + trade);
        }
        System.out.println();

        // === Шаг 7: Финальные балансы ===
        System.out.println("7. Финальные балансы:");
        System.out.println("   Alice:");
        System.out.println("     - BTC: " + alice.getWallet().getBalance("BTC"));
        System.out.println("     - USDT: " + alice.getWallet().getBalance("USDT"));
        
        System.out.println("   Bob:");
        System.out.println("     - BTC: " + bob.getWallet().getBalance("BTC"));
        System.out.println("     - USDT: " + bob.getWallet().getBalance("USDT"));
        System.out.println();

        // === Шаг 8: Статистика стакана ===
        System.out.println("8. Статистика стакана BTC/USDT:");
        System.out.println("   " + exchange.getOrderBookStats("BTC/USDT"));
        System.out.println();

        System.out.println("=== Demo completed successfully! ===");
        
        // === Дополнительный сценарий: частичное исполнение ===
        System.out.println("\n=== Дополнительный сценарий: частичное исполнение ===\n");
        
        User charlie = exchange.registerUser("Charlie");
        charlie.deposit("BTC", new BigDecimal("10"));
        
        System.out.println("Charlie выставляет SELL ордер: 3 BTC по 46,000 USDT...");
        List<Trade> trades4 = exchange.submitOrder(
            charlie,
            "BTC",
            "USDT",
            Side.SELL,
            new BigDecimal("3"),
            new BigDecimal("46000")
        );
        System.out.println("   Сделок: " + trades4.size() + " (ожидает покупателя)");
        
        User dave = exchange.registerUser("Dave");
        dave.deposit("USDT", new BigDecimal("50000"));
        
        System.out.println("Dave выставляет BUY ордер: 1 BTC по 46,000 USDT (частичное исполнение)...");
        List<Trade> trades5 = exchange.submitOrder(
            dave,
            "BTC",
            "USDT",
            Side.BUY,
            new BigDecimal("1"),
            new BigDecimal("46000")
        );
        System.out.println("   Сделок исполнено: " + trades5.size());
        for (Trade trade : trades5) {
            System.out.println("   >>> Сделка: " + trade);
        }
        
        System.out.println("\n   Charlie остаток в стакане: 2 BTC по 46,000 USDT");
        System.out.println("   Charlie баланс BTC: " + charlie.getWallet().getBalance("BTC"));
        System.out.println("   Charlie зарезервировано BTC: " + charlie.getWallet().getReserved("BTC"));
        System.out.println("   Dave баланс BTC: " + dave.getWallet().getBalance("BTC"));
        System.out.println("   Dave баланс USDT: " + dave.getWallet().getBalance("USDT"));
        
        System.out.println("\n=== Все сценарии завершены! ===");
    }
}
