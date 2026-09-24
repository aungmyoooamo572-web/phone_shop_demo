package com.example.demo.order.dao;

import com.example.demo.order.entity.Order;
import com.example.demo.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByUserId(UUID userId);

    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Order> findByStatus(OrderStatus status);

}
