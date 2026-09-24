package com.example.demo.order.service;

import com.example.demo.cart.dao.CartRepository;
import com.example.demo.cart.entity.Cart;
import com.example.demo.cart.entity.CartItem;
import com.example.demo.catalog.entity.PhoneVariant;
import com.example.demo.order.dao.OrderRepository;
import com.example.demo.order.dto.CreateOrderDto;
import com.example.demo.order.dto.OrderItemResponseDto;
import com.example.demo.order.dto.OrderResponseDto;
import com.example.demo.order.entity.Order;
import com.example.demo.order.entity.OrderItem;
import com.example.demo.order.entity.OrderStatus;
import com.example.demo.user.entity.User;
import com.example.demo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserService userService;

    public OrderResponseDto createOrder(
            String username,
            CreateOrderDto dto
    ) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(dto.getShippingAddress());
        order.setNotes(dto.getNotes());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            PhoneVariant variant = cartItem.getPhoneVariant();

            if (!variant.isActive()) {
                throw new RuntimeException("Phone variant is not active: " + variant.getSku());
            }

            if (variant.isDeleted()) {
                throw new RuntimeException("Product is no longer available: " + variant.getSku());
            }

            if (cartItem.getQuantity() > variant.getStock()) {
                throw new RuntimeException("Product quantity exceeds stock: " + variant.getSku());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setPhoneVariant(variant);
            orderItem.setProductName(variant.getPhone().getName());
            orderItem.setSku(variant.getSku());
            orderItem.setPrice(variant.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());

            BigDecimal subtotal = variant.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            totalAmount = totalAmount.add(subtotal);

            order.getItems().add(orderItem);
        }

        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);

        return toDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto findById(
            String username,
            UUID orderId
    ) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not allowed to access this order");
        }

        return toDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> findMyOrders(
            String username
    ) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return orderRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    public OrderResponseDto updateOrderStatus(
            UUID orderId,
            OrderStatus newStatus
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        OrderStatus currentStatus = order.getStatus();

        if (!isValidTransition(currentStatus, newStatus)) {
            throw new RuntimeException(
                    "Invalid order status transition: "
                            + currentStatus + " -> " + newStatus
            );
        }

        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        return toDto(savedOrder);
    }

    private boolean isValidTransition(
            OrderStatus currentStatus,
            OrderStatus newStatus
    ) {
        return switch (currentStatus) {
            case PENDING ->
                    newStatus == OrderStatus.CONFIRMED
                            || newStatus == OrderStatus.CANCELLED;
            case CONFIRMED ->
                    newStatus == OrderStatus.PROCESSING
                            || newStatus == OrderStatus.CANCELLED;
            case PROCESSING ->
                    newStatus == OrderStatus.SHIPPED;
            case SHIPPED ->
                    newStatus == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED ->
                    false;
        };
    }

    public OrderResponseDto updateTrackingNumber(
            UUID orderId,
            String trackingNumber
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new RuntimeException(
                    "Tracking number can only be added when order is SHIPPED"
            );
        }

        order.setTrackingNumber(trackingNumber);
        Order savedOrder = orderRepository.save(order);

        return toDto(savedOrder);
    }

    private OrderResponseDto toDto(Order order) {
        List<OrderItemResponseDto> items = order.getItems()
                .stream()
                .map(this::toItemDto)
                .toList();

        return new OrderResponseDto(
                order.getId(),
                order.getUser().getId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getShippingAddress(),
                order.getTrackingNumber(),
                order.getNotes(),
                order.getCreatedAt(),
                items
        );
    }

    private OrderItemResponseDto toItemDto(OrderItem item) {
        BigDecimal subtotal = item.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        return new OrderItemResponseDto(
                item.getId(),
                item.getPhoneVariant().getId(),
                item.getProductName(),
                item.getSku(),
                item.getPrice(),
                item.getQuantity(),
                subtotal
        );
    }

}
