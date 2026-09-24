package com.example.demo.payment.controller;

import com.example.demo.payment.dto.CreatePaymentDto;
import com.example.demo.payment.dto.PaymentResponseDto;
import com.example.demo.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    // CUSTOMER - Create Payment
    @PostMapping
    public PaymentResponseDto createPayment(
            Authentication authentication,
            @Valid @RequestBody CreatePaymentDto dto
    ) {
        return paymentService.createPayment(
                authentication.getName(),
                dto
        );
    }

    // CUSTOMER - Get payments by order
    @GetMapping("/order/{orderId}")
    public List<PaymentResponseDto> findByOrderId(
            Authentication authentication,
            @PathVariable UUID orderId
    ) {
        return paymentService.findByOrderId(
                authentication.getName(),
                orderId
        );
    }

    // CUSTOMER - Get payment by ID
    @GetMapping("/{paymentId}")
    public PaymentResponseDto findById(
            Authentication authentication,
            @PathVariable UUID paymentId
    ) {
        return paymentService.findById(
                authentication.getName(),
                paymentId
        );
    }

    // ADMIN - Verify Payment
    @PostMapping("/{paymentId}/verify")
    public PaymentResponseDto verifyPayment(
            @PathVariable UUID paymentId
    ) {
        return paymentService.verifyPayment(paymentId);
    }

    // ADMIN - Reject Payment
    @PostMapping("/{paymentId}/reject")
    public PaymentResponseDto rejectPayment(
            @PathVariable UUID paymentId
    ) {
        return paymentService.rejectPayment(paymentId);
    }

}
