package com.exchange.config;

import com.alex.fin.core.domain.common.InstrumentId;
import com.alex.fin.core.domain.common.Price;
import com.alex.fin.core.domain.common.Quantity;
import com.alex.fin.core.domain.common.CurrencyCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

public class JpaConverterConfig {

    @Converter(autoApply = true)
    public static class InstrumentIdConverter implements AttributeConverter<InstrumentId, String> {
        @Override
        public String convertToDatabaseColumn(InstrumentId attribute) {
            return attribute == null ? null : attribute.value();
        }

        @Override
        public InstrumentId convertToEntityAttribute(String dbData) {
            return dbData == null ? null : new InstrumentId(dbData);
        }
    }

    @Converter(autoApply = true)
    public static class QuantityConverter implements AttributeConverter<Quantity, BigDecimal> {
        @Override
        public BigDecimal convertToDatabaseColumn(Quantity attribute) {
            return attribute == null ? null : attribute.value();
        }

        @Override
        public Quantity convertToEntityAttribute(BigDecimal dbData) {
            return dbData == null ? null : new Quantity(dbData);
        }
    }

    /**
     * Converter for Price. 
     * NOTE: Since Price requires a CurrencyCode, and JPA AttributeConverter 
     * only maps to a single column, we lose the currency information here.
     * In the context of Exchange, we assume the currency is the quote asset.
     */
    @Converter(autoApply = true)
    public static class PriceConverter implements AttributeConverter<Price, BigDecimal> {
        @Override
        public BigDecimal convertToDatabaseColumn(Price attribute) {
            return attribute == null ? null : attribute.value();
        }

        @Override
        public Price convertToEntityAttribute(BigDecimal dbData) {
            // We use a placeholder currency because we don't know it here.
            // This is a trade-off to maintain the single-column database schema.
            return dbData == null ? null : new Price(dbData, new CurrencyCode("USD"));
        }
    }
}
