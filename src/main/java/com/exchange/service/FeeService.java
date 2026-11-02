package com.exchange.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class FeeService {
    // В реальности эти значения должны браться из конфига или БД (в зависимости от уровня пользователя)
    private static final BigDecimal TAKER_FEE_RATE = new BigDecimal("0.001"); // 0.1%
    private static final BigDecimal MAKER_FEE_RATE = new BigDecimal("0.0005"); // 0.05%

    public BigDecimal calculateTakerFee(BigDecimal amount) {
        return calculateFee(amount, TAKER_FEE_RATE);
    }

    public BigDecimal calculateMakerFee(BigDecimal amount) {
        return calculateFee(amount, MAKER_FEE_RATE);
    }

    private BigDecimal calculateFee(BigDecimal amount, BigDecimal rate) {
        return amount.multiply(rate);
    }
}
