package com.example.demo.payment.dto;

import com.example.demo.payment.entity.PaymentProvider;
import com.example.demo.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {

    private UUID id;
    private UUID orderId;
    private BigDecimal amount;
    private PaymentProvider provider;
    private String transactionId;
    private String paymentSlipUrl;
    private PaymentStatus status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

}
