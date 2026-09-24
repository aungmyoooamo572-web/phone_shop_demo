package com.example.demo.payment.service;

import com.example.demo.catalog.entity.PhoneVariant;
import com.example.demo.order.dao.OrderRepository;
import com.example.demo.order.entity.Order;
import com.example.demo.order.entity.OrderItem;
import com.example.demo.order.entity.OrderStatus;
import com.example.demo.payment.dao.PaymentRepository;
import com.example.demo.payment.dto.CreatePaymentDto;
import com.example.demo.payment.dto.PaymentResponseDto;
import com.example.demo.payment.entity.Payment;
import com.example.demo.payment.entity.PaymentStatus;
import com.example.demo.user.entity.User;
import com.example.demo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserService userService;

    public PaymentResponseDto createPayment(
            String username,
            CreatePaymentDto dto
    ) {
        User user = userService.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() ->
                        new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "You are not allowed to make payment for this order"
            );
        }

        if (paymentRepository.existsByOrderIdAndStatus(
                order.getId(),
                PaymentStatus.PENDING
        )) {
            throw new RuntimeException(
                    "Payment is already pending for this order"
            );
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setProvider(dto.getProvider());
        payment.setTransactionId(dto.getTransactionId());
        payment.setPaymentSlipUrl(dto.getPaymentSlipUrl());
        payment.setStatus(PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.save(payment);

        return toDto(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> findByOrderId(
            String username,
            UUID orderId
    ) {
        User user = userService.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "You are not allowed to make payment for this order"
            );
        }

        return paymentRepository
                .findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponseDto findById(
            String username,
            UUID paymentId
    ) {
        User user = userService.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found"));

        if (!payment.getOrder().getUser().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "You are not allowed to make payment for this order"
            );
        }

        return toDto(payment);
    }

    public PaymentResponseDto verifyPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.VERIFIED) {
            throw new RuntimeException("Payment is already verified");
        }

        if (payment.getStatus() == PaymentStatus.REJECTED) {
            throw new RuntimeException("Rejected payment cannot be verified");
        }

        Order order = payment.getOrder();

        for (OrderItem orderItem : order.getItems()) {
            PhoneVariant variant = orderItem.getPhoneVariant();

            if (!variant.isActive() || variant.isDeleted()) {
                throw new RuntimeException(
                        "Product is no longer available: " + variant.getSku()
                );
            }

            if (orderItem.getQuantity() > variant.getStock()) {
                throw new RuntimeException(
                        "Not enough stock for: " + variant.getSku()
                );
            }
        }

        for (OrderItem orderItem : order.getItems()) {
            PhoneVariant variant = orderItem.getPhoneVariant();
            variant.setStock(variant.getStock() - orderItem.getQuantity());
        }

        payment.setStatus(PaymentStatus.VERIFIED);
        payment.setPaidAt(LocalDateTime.now());

        order.setStatus(OrderStatus.CONFIRMED);

        Payment savedPayment = paymentRepository.save(payment);

        return toDto(savedPayment);
    }

    public PaymentResponseDto rejectPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.VERIFIED) {
            throw new RuntimeException("Verified payment cannot be rejected");
        }

        if (payment.getStatus() == PaymentStatus.REJECTED) {
            throw new RuntimeException("Payment is already rejected");
        }

        payment.setStatus(PaymentStatus.REJECTED);

        Payment savedPayment = paymentRepository.save(payment);

        return toDto(savedPayment);
    }

    private PaymentResponseDto toDto(Payment payment) {
        return new PaymentResponseDto(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getAmount(),
                payment.getProvider(),
                payment.getTransactionId(),
                payment.getPaymentSlipUrl(),
                payment.getStatus(),
                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }

}
