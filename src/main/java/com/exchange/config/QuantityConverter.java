package com.exchange.config;

import com.alex.fin.core.domain.common.Quantity;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

@Converter(autoApply = true)
public class QuantityConverter implements AttributeConverter<Quantity, BigDecimal> {
    @Override
    public BigDecimal convertToDatabaseColumn(Quantity attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public Quantity convertToEntityAttribute(BigDecimal dbData) {
        return dbData == null ? null : new Quantity(dbData);
    }
}
