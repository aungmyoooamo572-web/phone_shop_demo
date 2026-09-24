package com.example.demo.order.dto;

import com.example.demo.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {

    private UUID id;
    private UUID userId;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String shippingAddress;
    private String trackingNumber;
    private String notes;
    private LocalDateTime createdAt;
    private List<OrderItemResponseDto> items;

}
