package com.example.demo.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderDto {

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    private String notes;

}
