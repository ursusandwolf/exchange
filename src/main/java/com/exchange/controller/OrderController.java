package com.exchange.controller;

import com.exchange.dto.AuthUser;
import com.exchange.dto.OcoOrderRequest;
import com.exchange.dto.OcoOrderResponse;
import com.exchange.dto.OrderRequest;
import com.exchange.dto.TradeResponse;
import com.exchange.engine.OrderBook;
import com.exchange.mapper.OrderMapper;
import com.exchange.mapper.TradeMapper;
import com.exchange.model.Trade;
import com.exchange.model.User;
import com.exchange.service.AccountService;
import com.exchange.service.MatchingManager;
import com.exchange.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final MatchingManager matchingManager;
    private final AccountService accountService;
    private final TradeMapper tradeMapper;
    private final OrderMapper orderMapper;

    @PostMapping("/submit")
    public List<TradeResponse> submitOrder(@AuthenticationPrincipal AuthUser currentUser, @RequestBody @Valid OrderRequest request) {
        User user = accountService.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        List<Trade> trades = orderService.submitOrder(user, request);

        return trades.stream().map(tradeMapper::toResponse).toList();
    }

    @PostMapping("/oco")
    public OcoOrderResponse submitOcoOrder(@AuthenticationPrincipal AuthUser currentUser, @RequestBody @Valid OcoOrderRequest request) {
        User user = accountService.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        return orderService.submitOcoOrder(user, request);
    }

    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(@AuthenticationPrincipal AuthUser currentUser, @PathVariable String orderId) {
        User user = accountService.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
        
        boolean cancelled = orderService.cancelOrder(user.getId(), orderId);
        return cancelled ? "Order cancelled successfully" : "Order could not be cancelled (already filled or not found)";
    }

    @GetMapping("/history")
    public List<com.exchange.dto.OrderResponse> getOrderHistory(@AuthenticationPrincipal AuthUser currentUser) {
        User user = accountService.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
        
        return orderService.getUserOrders(user.getId()).stream()
            .map(orderMapper::toResponse)
            .toList();
    }

    @GetMapping("/stats")
    public String getStats(@RequestParam String symbol) {
        OrderBook orderBook = matchingManager.getOrderBookBySymbol(symbol);
        if (orderBook == null) return "Symbol not found";
        return "OrderBook " + symbol + ": " + orderBook.getBids().size() + " price levels";
    }
}
