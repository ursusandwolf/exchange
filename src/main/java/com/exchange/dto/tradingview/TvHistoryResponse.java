package com.exchange.dto.tradingview;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class TvHistoryResponse {
    @JsonProperty("s")
    String status; // "ok" or "no_data"
    
    @JsonProperty("t")
    List<Long> timestamps;
    
    @JsonProperty("o")
    List<BigDecimal> open;
    
    @JsonProperty("h")
    List<BigDecimal> high;
    
    @JsonProperty("l")
    List<BigDecimal> low;
    
    @JsonProperty("c")
    List<BigDecimal> close;
    
    @JsonProperty("v")
    List<BigDecimal> volume;
    
    @JsonProperty("errmsg")
    String errorMessage;

    public static TvHistoryResponse noData() {
        return TvHistoryResponse.builder().status("no_data").build();
    }
}
