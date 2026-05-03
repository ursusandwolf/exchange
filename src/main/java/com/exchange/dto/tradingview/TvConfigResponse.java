package com.exchange.dto.tradingview;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TvConfigResponse {
    @Builder.Default
    @JsonProperty("supports_search")
    boolean supportsSearch = true;
    @Builder.Default
    @JsonProperty("supports_group_request")
    boolean supportsGroupRequest = false;
    @Builder.Default
    @JsonProperty("supports_marks")
    boolean supportsMarks = false;
    @Builder.Default
    @JsonProperty("supports_timescale_marks")
    boolean supportsTimescaleMarks = false;
    @Builder.Default
    @JsonProperty("supports_time")
    boolean supportsTime = true;
    @Builder.Default
    @JsonProperty("exchanges")
    List<Exchange> exchanges = List.of(new Exchange("GeminiExchange", "Gemini", "Gemini Simulation Exchange"));
    @Builder.Default
    @JsonProperty("symbols_types")
    List<SymbolType> symbolsTypes = List.of(new SymbolType("crypto", "Crypto"));
    @Builder.Default
    @JsonProperty("supported_resolutions")
    List<String> supportedResolutions = List.of("1", "5", "15", "30", "60", "D");

    public record Exchange(String value, String name, String desc) {}
    public record SymbolType(String name, String value) {}
}
