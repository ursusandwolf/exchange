package com.exchange.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FeeServiceTest {

    private final FeeService feeService = new FeeService();

    @Test
    void calculateTakerFee_usesPercentRate() {
        assertThat(feeService.calculateTakerFee(new BigDecimal("1000")))
                .isEqualByComparingTo("1.00000000");
    }

    @Test
    void calculateMakerFee_usesPercentRate() {
        assertThat(feeService.calculateMakerFee(new BigDecimal("1000")))
                .isEqualByComparingTo("0.50000000");
    }
}
