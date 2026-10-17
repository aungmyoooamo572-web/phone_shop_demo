package com.example.demo.order.service;

import com.example.demo.cart.dao.CartRepository;
import com.example.demo.cart.entity.Cart;
import com.example.demo.cart.entity.CartItem;
import com.example.demo.catalog.entity.PhoneVariant;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ConflictException;
import com.example.demo.order.dao.OrderRepository;
import com.example.demo.order.dto.CreateOrderDto;
import com.example.demo.order.entity.Order;
import com.example.demo.order.entity.OrderStatus;
import com.example.demo.user.entity.Role;
import com.example.demo.user.entity.User;
import com.example.demo.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Representative business failures of {@link OrderService}: they used to escape
 * as plain {@link RuntimeException} (HTTP 500) and must now be mapped to the
 * right client error.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceBusinessErrorTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private OrderService orderService;

    private User customer;

    private Cart cart;

    private PhoneVariant variant;

    private CreateOrderDto dto;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(UUID.randomUUID());
        customer.setUsername("customer-one");
        customer.setRole(Role.CUSTOMER);

        cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUser(customer);

        variant = new PhoneVariant();
        variant.setId(UUID.randomUUID());
        variant.setSku("SKU-1");
        variant.setStock(10);
        variant.setPrice(new BigDecimal("100000.00"));

        dto = new CreateOrderDto();
        dto.setShippingAddress("Yangon");
    }

    private void signedInCustomerWithCart() {
        when(userService.findByUsername("customer-one")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId(customer.getId())).thenReturn(Optional.of(cart));
    }

    private void cartContains(int quantity) {
        CartItem item = new CartItem();
        item.setId(UUID.randomUUID());
        item.setCart(cart);
        item.setPhoneVariant(variant);
        item.setQuantity(quantity);
        cart.getItems().add(item);
    }

    @Test
    @DisplayName("An empty cart is a 400, not a 500")
    void emptyCartIsBadRequest() {
        signedInCustomerWithCart();

        assertThatThrownBy(() -> orderService.createOrder("customer-one", dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cart is empty");
    }

    @Test
    @DisplayName("Ordering more than the available stock is a 409")
    void quantityAboveStockIsConflict() {
        signedInCustomerWithCart();
        cartContains(11); // stock is 10

        assertThatThrownBy(() -> orderService.createOrder("customer-one", dto))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Product quantity exceeds stock: SKU-1");
    }

    @Test
    @DisplayName("Ordering an inactive variant is a 409")
    void inactiveVariantIsConflict() {
        signedInCustomerWithCart();
        cartContains(1);
        variant.setActive(false);

        assertThatThrownBy(() -> orderService.createOrder("customer-one", dto))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Phone variant is not active: SKU-1");
    }

    @Test
    @DisplayName("A deleted variant is a 409")
    void deletedVariantIsConflict() {
        signedInCustomerWithCart();
        cartContains(1);
        variant.setDeleted(true);

        assertThatThrownBy(() -> orderService.createOrder("customer-one", dto))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Product is no longer available: SKU-1");
    }

    @Test
    @DisplayName("An impossible status transition is a 409")
    void invalidStatusTransitionIsConflict() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setUser(customer);
        order.setStatus(OrderStatus.DELIVERED);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(
                order.getId(),
                OrderStatus.CONFIRMED
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Invalid order status transition: DELIVERED -> CONFIRMED");
    }

    @Test
    @DisplayName("Adding tracking before SHIPPED is a 409")
    void trackingBeforeShippedIsConflict() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setUser(customer);
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateTrackingNumber(order.getId(), "TRACK-1"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Tracking number can only be added when order is SHIPPED");
    }
}
