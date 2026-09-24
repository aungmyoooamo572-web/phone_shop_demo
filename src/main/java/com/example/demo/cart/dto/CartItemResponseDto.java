package com.example.demo.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponseDto {

    private UUID id;
    private UUID phoneVariantId;
    private String phoneName;
    private String ram;
    private String storage;
    private String color;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;

}
