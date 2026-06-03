package com.exchange.controller;

import com.exchange.dto.OrderRequest;
import com.exchange.dto.TradeResponse;
import com.exchange.engine.OrderBook;
import com.exchange.model.Trade;
import com.exchange.model.User;
import com.exchange.repository.UserRepository;
import com.exchange.service.MatchingManager;
import com.exchange.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final MatchingManager matchingManager;
    private final UserRepository userRepository;

    @PostMapping("/submit")
    public List<TradeResponse> submitOrder(@RequestBody @Valid OrderRequest request) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        List<Trade> trades = orderService.submitOrder(user, request);

        return trades.stream()
            .map(t -> new TradeResponse(
                t.getId(),
                t.getBuyOrder().getId(),
                t.getSellOrder().getId(),
                t.getPrice().value(),
                t.getQuantity().value(),
                t.getTotalAmount(),
                LocalDateTime.ofInstant(t.getTimestamp(), ZoneId.systemDefault())
            ))
            .toList();
    }

    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(@PathVariable String orderId) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
        
        boolean cancelled = orderService.cancelOrder(user.getId(), orderId);
        return cancelled ? "Order cancelled successfully" : "Order could not be cancelled (already filled or not found)";
    }

    @GetMapping("/history")
    public List<com.exchange.dto.OrderResponse> getOrderHistory() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
        
        return orderService.getUserOrders(user.getId()).stream()
            .map(o -> new com.exchange.dto.OrderResponse(
                o.getId(),
                o.getBaseAsset().value(),
                o.getQuoteAsset().value(),
                o.getSide(),
                o.getType(),
                o.getQuantity().value(),
                o.getPrice() != null ? o.getPrice().value() : null,
                o.getFilledQuantity().value(),
                o.getStatus(),
                o.getCreatedAt()
            ))
            .toList();
    }

    @GetMapping("/stats")
    public String getStats(@RequestParam String symbol) {
        OrderBook orderBook = matchingManager.getOrderBookBySymbol(symbol);
        if (orderBook == null) return "Symbol not found";
        return "OrderBook " + symbol + ": " + orderBook.getBids().size() + " price levels";
    }
}
