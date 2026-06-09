package com.exchange.dto;

import java.util.List;

public record OcoOrderResponse(
    String ocoGroupId,
    List<String> orderIds
) {}
