package com.exchange.dto.tradingview;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TvSymbolResponse {
    String name;
    @JsonProperty("ticker")
    String ticker;
    @JsonProperty("description")
    String description;
    @Builder.Default
    @JsonProperty("type")
    String type = "crypto";
    @Builder.Default
    @JsonProperty("session")
    String session = "24x7";
    @Builder.Default
    @JsonProperty("exchange")
    String exchange = "Gemini";
    @Builder.Default
    @JsonProperty("listed_exchange")
    String listedExchange = "Gemini";
    @Builder.Default
    @JsonProperty("timezone")
    String timezone = "Etc/UTC";
    @Builder.Default
    @JsonProperty("minmov")
    int minMov = 1;
    @Builder.Default
    @JsonProperty("pricescale")
    int priceScale = 100;
    @Builder.Default
    @JsonProperty("has_intraday")
    boolean hasIntraday = true;
    @Builder.Default
    @JsonProperty("has_no_volume")
    boolean hasNoVolume = false;
    @Builder.Default
    @JsonProperty("supported_resolutions")
    List<String> supportedResolutions = List.of("1", "5", "15", "30", "60", "D");
    @Builder.Default
    @JsonProperty("pricescale_multiplier")
    int priceScaleMultiplier = 1;
}
