package com.exchange.mapper;

import com.exchange.dto.OrderResponse;
import com.exchange.model.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {
    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getBaseAsset().value(),
                order.getQuoteAsset().value(),
                order.getSide(),
                order.getType(),
                order.getQuantity().value(),
                order.getPrice() != null ? order.getPrice().value() : null,
                order.getPriceLimit() != null ? order.getPriceLimit().value() : null,
                order.getTriggerPrice() != null ? order.getTriggerPrice().value() : null,
                order.getOcoGroupId(),
                order.getFilledQuantity().value(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
