package com.exchange.mapper;

import com.exchange.dto.TransactionRecordResponse;
import com.exchange.model.TransactionRecord;
import org.springframework.stereotype.Component;

@Component
public class TransactionRecordMapper {
    public TransactionRecordResponse toResponse(TransactionRecord record) {
        return new TransactionRecordResponse(
                record.getId(),
                record.getUserId(),
                record.getAsset(),
                record.getAmount(),
                record.getType(),
                record.getDescription(),
                record.getTimestamp()
        );
    }
}
