package com.example.demo.order.controller;

import com.example.demo.order.dto.CreateOrderDto;
import com.example.demo.order.dto.OrderResponseDto;
import com.example.demo.order.dto.UpdateOrderStatusDto;
import com.example.demo.order.dto.UpdateTrackingNumberDto;
import com.example.demo.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public OrderResponseDto createOrder(
            Authentication authentication,
            @Valid @RequestBody CreateOrderDto dto
    ) {
        return orderService.createOrder(
                authentication.getName(),
                dto
        );
    }

    @GetMapping
    public List<OrderResponseDto> findMyOrders(
            Authentication authentication
    ) {
        return orderService.findMyOrders(
                authentication.getName()
        );
    }

    @GetMapping("/{orderId}")
    public OrderResponseDto findById(
            Authentication authentication,
            @PathVariable UUID orderId
    ) {
        return orderService.findById(
                authentication.getName(),
                orderId
        );
    }

    @PutMapping("/{orderId}/status")
    public OrderResponseDto updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusDto dto
    ) {
        return orderService.updateOrderStatus(
                orderId,
                dto.getStatus()
        );
    }

    @PutMapping("/{orderId}/tracking")
    public OrderResponseDto updateTrackingNumber(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateTrackingNumberDto dto
    ) {
        return orderService.updateTrackingNumber(
                orderId,
                dto.getTrackingNumber()
        );
    }
}
