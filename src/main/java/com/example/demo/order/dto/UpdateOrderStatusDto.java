package com.example.demo.order.dto;

import com.example.demo.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrderStatusDto {

    @NotNull(message = "Order status is required")
    private OrderStatus status;

}
