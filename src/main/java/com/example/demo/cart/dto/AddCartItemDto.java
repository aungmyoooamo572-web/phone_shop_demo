package com.example.demo.cart.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AddCartItemDto {

    @NotNull(message = "Phone variant is required")
    private UUID phoneVariantId;

}
