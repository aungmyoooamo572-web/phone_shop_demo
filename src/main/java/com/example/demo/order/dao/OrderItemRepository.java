package com.example.demo.order.dao;

import com.example.demo.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderId(UUID orderId);

    List<OrderItem> findByPhoneVariantId(UUID phoneVariantId);

    /*
     * Returns (phoneVariantId, quantity) pairs as scalars. Deliberately avoids
     * loading the PhoneVariant entities so that the variant rows are first read
     * by the locking query in PaymentService and never by a stale non-locking
     * read beforehand.
     */
    @Query("""
            select i.phoneVariant.id, i.quantity
            from OrderItem i
            where i.order.id = :orderId
            """)
    List<Object[]> findVariantQuantitiesByOrderId(@Param("orderId") UUID orderId);

}
