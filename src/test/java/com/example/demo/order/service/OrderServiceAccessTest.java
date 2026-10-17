package com.example.demo.order.service;

import com.example.demo.cart.dao.CartRepository;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.order.dao.OrderRepository;
import com.example.demo.order.dto.OrderResponseDto;
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
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the order read rules: ADMIN may read any order, a CUSTOMER only their own.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceAccessTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private OrderService orderService;

    private UUID orderId;

    private User owner;

    private Order order;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        owner = newUser("customer-one", Role.CUSTOMER);

        order = new Order();
        order.setId(orderId);
        order.setUser(owner);
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress("Yangon");
        order.setTotalAmount(new BigDecimal("1500000"));
    }

    private User newUser(String username, Role role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setRole(role);
        return user;
    }

    private void signedInAs(User user) {
        when(userService.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("ADMIN can read another customer's order")
    void adminCanReadAnyOrder() {
        signedInAs(newUser("admin", Role.ADMIN));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponseDto dto = orderService.findById("admin", orderId);

        assertThat(dto.getUserId()).isEqualTo(owner.getId());
    }

    @Test
    @DisplayName("The owner can still read their own order")
    void ownerCanReadOwnOrder() {
        signedInAs(owner);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThat(orderService.findById("customer-one", orderId).getUserId())
                .isEqualTo(owner.getId());
    }

    @Test
    @DisplayName("A different customer is still denied")
    void otherCustomerIsDenied() {
        signedInAs(newUser("customer-two", Role.CUSTOMER));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.findById("customer-two", orderId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You are not allowed to access this order");
    }

    @Test
    @DisplayName("A missing order raises ResourceNotFoundException (404)")
    void missingOrderIsNotFound() {
        signedInAs(newUser("admin", Role.ADMIN));
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById("admin", orderId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found");
    }

    @Test
    @DisplayName("findAllOrders returns every customer's order for an ADMIN")
    void adminCanListAllOrders() {
        Order otherOrder = new Order();
        otherOrder.setId(UUID.randomUUID());
        otherOrder.setUser(newUser("customer-two", Role.CUSTOMER));
        otherOrder.setStatus(OrderStatus.CONFIRMED);
        otherOrder.setShippingAddress("Mandalay");
        otherOrder.setTotalAmount(new BigDecimal("900000"));

        signedInAs(newUser("admin", Role.ADMIN));
        when(orderRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(otherOrder, order));

        List<OrderResponseDto> all = orderService.findAllOrders("admin");

        assertThat(all).hasSize(2);
        assertThat(all).extracting(OrderResponseDto::getUserId)
                .containsExactly(otherOrder.getUser().getId(), owner.getId());
        assertThat(all).extracting(OrderResponseDto::getStatus)
                .containsExactly(OrderStatus.CONFIRMED, OrderStatus.PENDING);
    }

    @Test
    @DisplayName("findAllOrders is denied for a CUSTOMER and never touches the repository")
    void customerCannotListAllOrders() {
        signedInAs(owner);

        assertThatThrownBy(() -> orderService.findAllOrders("customer-one"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You are not allowed to view all orders");

        verify(orderRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("findAllOrders raises ResourceNotFoundException when the caller is unknown")
    void findAllOrdersWithUnknownCallerIsNotFound() {
        when(userService.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findAllOrders("ghost"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }
}
