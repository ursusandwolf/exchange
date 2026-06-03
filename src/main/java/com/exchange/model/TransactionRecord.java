package com.exchange.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transaction_records")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TransactionRecord {
    @Id
    private String id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private String asset;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private String type; // DEPOSIT, CREDIT, DEBIT, RESERVE, UNRESERVE, DEDUCT_RESERVED
    
    @Column(nullable = false)
    private String description;
    
    @Column(nullable = false)
    private Instant timestamp;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (timestamp == null) timestamp = Instant.now();
    }
}
