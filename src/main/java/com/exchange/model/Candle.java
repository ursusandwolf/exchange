package com.exchange.model;

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
    
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;

    public void update(BigDecimal price, BigDecimal quantity) {
        this.close = price;
        this.volume = this.volume.add(quantity);
        if (price.compareTo(this.high) > 0) this.high = price;
        if (price.compareTo(this.low) < 0) this.low = price;
    }
}
