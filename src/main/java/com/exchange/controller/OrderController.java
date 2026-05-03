package com.exchange.controller;

import com.exchange.enums.Side;
import com.exchange.model.Trade;
import com.exchange.model.User;
import com.exchange.service.ExchangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final ExchangeService exchangeService;

    @PostMapping("/submit")
    public List<Trade> submitOrder(
            @RequestParam String userId,
            @RequestParam String baseAsset,
            @RequestParam String quoteAsset,
            @RequestParam Side side,
            @RequestParam BigDecimal quantity,
            @RequestParam(required = false) BigDecimal price) {
        
        User user = exchangeService.getUser(userId);
        return exchangeService.submitOrder(user, baseAsset, quoteAsset, side, quantity, price);
    }

    @GetMapping("/stats")
    public String getStats(@RequestParam String symbol) {
        return exchangeService.getOrderBookStats(symbol);
    }
}
