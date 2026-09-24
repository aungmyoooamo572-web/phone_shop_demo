package com.example.demo.payment.dto;

import com.example.demo.payment.entity.PaymentProvider;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreatePaymentDto {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotNull(message = "Payment provider is required")
    private PaymentProvider provider;

    private String transactionId;

    private String paymentSlipUrl;

}
