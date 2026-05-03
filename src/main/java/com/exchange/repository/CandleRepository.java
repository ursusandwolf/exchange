package com.exchange.repository;

import com.exchange.model.Candle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CandleRepository extends JpaRepository<Candle, String> {
    Optional<Candle> findBySymbolAndIntervalAndOpenTime(String symbol, String interval, LocalDateTime openTime);
    List<Candle> findBySymbolAndIntervalOrderByOpenTimeDesc(String symbol, String interval);
}
