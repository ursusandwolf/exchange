package com.exchange.mapper;

import com.exchange.dto.TradeResponse;
import com.exchange.model.Trade;
import org.springframework.stereotype.Component;

@Component
public class TradeMapper {
    public TradeResponse toResponse(Trade trade) {
        return new TradeResponse(
                trade.getId(),
                trade.getBuyOrder().getId(),
                trade.getSellOrder().getId(),
                trade.getPrice().value(),
                trade.getQuantity().value(),
                trade.getTotalAmount(),
                trade.getTimestamp()
        );
    }
}
