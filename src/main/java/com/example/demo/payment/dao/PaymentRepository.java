package com.example.demo.payment.dao;

import com.example.demo.payment.entity.Payment;
import com.example.demo.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrderId(UUID orderId);

    List<Payment> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    List<Payment> findByStatus(PaymentStatus status);

    boolean existsByOrderIdAndStatus(UUID orderId, PaymentStatus status);

}
