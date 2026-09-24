package com.example.demo.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTrackingNumberDto {

    @NotBlank(message = "Tracking number is required")
    private String trackingNumber;

}
