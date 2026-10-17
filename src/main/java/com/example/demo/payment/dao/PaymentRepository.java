package com.example.demo.payment.dao;

import com.example.demo.payment.entity.Payment;
import com.example.demo.payment.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrderId(UUID orderId);

    List<Payment> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);

    List<Payment> findAllByOrderByCreatedAtDesc();

    boolean existsByOrderIdAndStatus(UUID orderId, PaymentStatus status);

    /*
     * Reads the payment row with an exclusive write lock (SELECT ... FOR UPDATE).
     * Concurrent verifications of the same payment therefore run one after the
     * other: the second one observes status = VERIFIED and is rejected before it
     * can deduct stock a second time.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") UUID id);

}
