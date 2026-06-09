package com.exchange.config;

import com.alex.fin.core.domain.common.InstrumentId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class InstrumentIdConverter implements AttributeConverter<InstrumentId, String> {
    @Override
    public String convertToDatabaseColumn(InstrumentId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public InstrumentId convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new InstrumentId(dbData);
    }
}
