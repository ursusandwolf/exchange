package com.exchange.repository;

import com.exchange.model.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, String> {
    List<TransactionRecord> findByUserIdOrderByTimestampDesc(String userId);
}
