package com.exchange.repository;

import com.exchange.enums.OrderStatus;
import com.exchange.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByUserId(String userId);
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Order> findByStatusInOrderByCreatedAtAsc(List<OrderStatus> statuses);
    long countByStatusIn(List<OrderStatus> statuses);
    List<Order> findByOcoGroupIdAndUserIdAndStatusIn(String ocoGroupId, String userId, List<OrderStatus> statuses);
    List<Order> findByOcoGroupId(String ocoGroupId);
}
