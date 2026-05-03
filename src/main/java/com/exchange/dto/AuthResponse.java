package com.exchange.dto;

public record AuthResponse(
    String token,
    String username
) {}
