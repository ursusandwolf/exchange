package com.exchange.config;

import com.alex.fin.core.domain.common.CurrencyCode;
import com.alex.fin.core.domain.common.Price;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

@Converter(autoApply = true)
public class PriceConverter implements AttributeConverter<Price, String> {
    private static final String SEPARATOR = "|";

    @Override
    public String convertToDatabaseColumn(Price attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.value().toPlainString() + SEPARATOR + attribute.currency().value();
    }

    @Override
    public Price convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        int separatorIndex = dbData.lastIndexOf(SEPARATOR);
        if (separatorIndex < 0) {
            throw new IllegalArgumentException("Invalid price payload: " + dbData);
        }

        BigDecimal value = new BigDecimal(dbData.substring(0, separatorIndex));
        CurrencyCode currency = new CurrencyCode(dbData.substring(separatorIndex + 1));
        return new Price(value, currency);
    }
}
