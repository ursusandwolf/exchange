package com.exchange.service;

import com.alex.fin.core.domain.common.Percent;
import com.alex.fin.core.domain.common.ValidationUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FeeService {
    private static final Percent TAKER_FEE_RATE = Percent.ofDecimal(new BigDecimal("0.001")); // 0.1%
    private static final Percent MAKER_FEE_RATE = Percent.ofDecimal(new BigDecimal("0.0005")); // 0.05%
    private static final int FEE_SCALE = 8;

    public BigDecimal calculateTakerFee(BigDecimal amount) {
        return calculateFee(amount, TAKER_FEE_RATE);
    }

    public BigDecimal calculateMakerFee(BigDecimal amount) {
        return calculateFee(amount, MAKER_FEE_RATE);
    }

    private BigDecimal calculateFee(BigDecimal amount, Percent rate) {
        if (amount == null || amount.signum() == 0) {
            return BigDecimal.ZERO;
        }
        ValidationUtils.validateNonNegative(amount, "amount");
        return amount.multiply(rate.value()).setScale(FEE_SCALE, RoundingMode.HALF_UP);
    }
}
