package com.exchange.service;

import com.alex.fin.core.domain.common.CurrencyCode;
import com.alex.fin.core.domain.common.Price;
import com.alex.fin.core.domain.common.Quantity;
import com.exchange.dto.CandleResponse;
import com.exchange.dto.OrderBookUpdate;
import com.exchange.dto.TradeResponse;
import com.exchange.engine.OrderBook;
import com.exchange.mapper.CandleMapper;
import com.exchange.mapper.TradeMapper;
import com.exchange.model.Candle;
import com.exchange.model.Trade;
import com.exchange.repository.CandleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataService {

    private final SimpMessagingTemplate messagingTemplate;
    private final CandleRepository candleRepository;
    private final CandleMapper candleMapper;
    private final TradeMapper tradeMapper;

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
        TradeResponse response = tradeMapper.toResponse(trade);

        String symbol = trade.getBuyOrder().getSymbol();
        messagingTemplate.convertAndSend("/topic/trades/" + symbol, response);
        
        updateCandles(symbol, trade.getPrice(), trade.getQuantity(), trade.getTimestamp());
    }

    private void updateCandles(String symbol, Price price, Quantity quantity, Instant timestamp) {
        // 1m candles
        LocalDateTime openTime = LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES);
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
                        .volume(new Quantity(BigDecimal.ZERO))
                        .build());

        candle.update(price, quantity);
        candleRepository.save(candle);

        // Broadcast candle update
        CandleResponse response = candleMapper.toResponse(candle);
        messagingTemplate.convertAndSend("/topic/candles/" + symbol + "/" + interval, response);
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
