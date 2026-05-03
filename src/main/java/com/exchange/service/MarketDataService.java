package com.exchange.service;

import com.exchange.dto.OrderBookUpdate;
import com.exchange.dto.TradeResponse;
import com.exchange.engine.OrderBook;
import com.exchange.model.Order;
import com.exchange.model.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import com.exchange.repository.CandleRepository;
import com.exchange.model.Candle;
import org.springframework.transaction.annotation.Transactional;
import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataService {

    private final SimpMessagingTemplate messagingTemplate;
    private final CandleRepository candleRepository;

    public void broadcastOrderBookUpdate(OrderBook orderBook) {
        OrderBookUpdate update = OrderBookUpdate.builder()
                .symbol(orderBook.getSymbol())
                .bids(convertPriceLevels(orderBook.getPriceLevels(com.exchange.enums.Side.BUY)))
                .asks(convertPriceLevels(orderBook.getPriceLevels(com.exchange.enums.Side.SELL)))
                .build();

        messagingTemplate.convertAndSend("/topic/orderbook/" + orderBook.getSymbol(), update);
    }

    @Transactional
    public void broadcastTrade(Trade trade) {
        TradeResponse response = new TradeResponse(
                trade.getId(),
                trade.getBuyOrder().getId(),
                trade.getSellOrder().getId(),
                trade.getPrice(),
                trade.getQuantity(),
                trade.getTotalAmount(),
                java.time.LocalDateTime.ofInstant(trade.getTimestamp(), java.time.ZoneOffset.UTC)
        );

        String symbol = trade.getBuyOrder().getSymbol();
        messagingTemplate.convertAndSend("/topic/trades/" + symbol, response);
        
        updateCandles(symbol, trade.getPrice(), trade.getQuantity(), response.timestamp());
    }

    private void updateCandles(String symbol, BigDecimal price, BigDecimal quantity, LocalDateTime timestamp) {
        // 1m candles
        LocalDateTime openTime = timestamp.truncatedTo(ChronoUnit.MINUTES);
        String interval = "1m";
        
        Candle candle = candleRepository.findBySymbolAndIntervalAndOpenTime(symbol, interval, openTime)
                .orElseGet(() -> Candle.builder()
                        .symbol(symbol)
                        .interval(interval)
                        .openTime(openTime)
                        .open(price)
                        .high(price)
                        .low(price)
                        .close(price)
                        .volume(BigDecimal.ZERO)
                        .build());
        
        candle.update(price, quantity);
        candleRepository.save(candle);
        
        // Broadcast candle update
        messagingTemplate.convertAndSend("/topic/candles/" + symbol + "/" + interval, candle);
    }

    private List<OrderBookUpdate.PriceLevel> convertPriceLevels(java.util.Map<BigDecimal, BigDecimal> levels) {
        return levels.entrySet().stream()
                .limit(20)
                .map(entry -> OrderBookUpdate.PriceLevel.builder()
                        .price(entry.getKey())
                        .quantity(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }
}
