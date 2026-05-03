package com.exchange.repository;

import com.exchange.model.Order;
import com.exchange.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<List<Order>> findByUserId(String userId); // Note: Simple userId query
    List<Order> findByStatusIn(List<OrderStatus> statuses);
}
