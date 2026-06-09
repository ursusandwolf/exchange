package com.exchange.model;

import com.alex.fin.core.domain.common.Price;
import com.alex.fin.core.domain.common.Quantity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "candles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candle {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String symbol;
    @jakarta.persistence.Column(name = "candle_interval")
    private String interval; // e.g., "1m"
    private LocalDateTime openTime;
    
    @jakarta.persistence.Column(length = 96)
    private Price open;
    @jakarta.persistence.Column(length = 96)
    private Price high;
    @jakarta.persistence.Column(length = 96)
    private Price low;
    @jakarta.persistence.Column(length = 96)
    private Price close;
    private Quantity volume;

    public void update(Price price, Quantity quantity) {
        this.close = price;
        this.volume = new Quantity(this.volume.value().add(quantity.value()));
        if (price.value().compareTo(this.high.value()) > 0) this.high = price;
        if (price.value().compareTo(this.low.value()) < 0) this.low = price;
    }
}
