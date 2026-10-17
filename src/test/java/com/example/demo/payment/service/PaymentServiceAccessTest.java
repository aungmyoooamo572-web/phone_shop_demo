package com.example.demo.payment.service;

import com.example.demo.catalog.dao.PhoneVariantRepository;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.order.dao.OrderItemRepository;
import com.example.demo.order.dao.OrderRepository;
import com.example.demo.order.entity.Order;
import com.example.demo.order.entity.OrderStatus;
import com.example.demo.payment.dao.PaymentRepository;
import com.example.demo.payment.dto.PaymentResponseDto;
import com.example.demo.payment.entity.Payment;
import com.example.demo.payment.entity.PaymentProvider;
import com.example.demo.payment.entity.PaymentStatus;
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
import static org.mockito.Mockito.when;

/**
 * Verifies the payment read rules: ADMIN may read any payment, a CUSTOMER only their own.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceAccessTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private PhoneVariantRepository phoneVariantRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private PaymentService paymentService;

    private UUID orderId;

    private UUID paymentId;

    private User owner;

    private Order order;

    private Payment payment;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        owner = newUser("customer-one", Role.CUSTOMER);

        order = new Order();
        order.setId(orderId);
        order.setUser(owner);
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress("Yangon");
        order.setTotalAmount(new BigDecimal("1500000"));

        payment = new Payment();
        payment.setId(paymentId);
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setProvider(PaymentProvider.KPAY);
        payment.setStatus(PaymentStatus.PENDING);
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
    @DisplayName("ADMIN can list the payments of another customer's order")
    void adminCanListPaymentsOfAnyOrder() {
        signedInAs(newUser("admin", Role.ADMIN));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(List.of(payment));

        List<PaymentResponseDto> payments = paymentService.findByOrderId("admin", orderId);

        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getOrderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("ADMIN can read another customer's payment by id")
    void adminCanReadAnyPayment() {
        signedInAs(newUser("admin", Role.ADMIN));
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThat(paymentService.findById("admin", paymentId).getId()).isEqualTo(paymentId);
    }

    @Test
    @DisplayName("The owner can still read their own payment")
    void ownerCanReadOwnPayment() {
        signedInAs(owner);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThat(paymentService.findById("customer-one", paymentId).getId()).isEqualTo(paymentId);
    }

    @Test
    @DisplayName("A different customer is denied for a payment they do not own")
    void otherCustomerIsDeniedOnPayment() {
        signedInAs(newUser("customer-two", Role.CUSTOMER));
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.findById("customer-two", paymentId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("A different customer is denied for another customer's order payments")
    void otherCustomerIsDeniedOnOrderPayments() {
        signedInAs(newUser("customer-two", Role.CUSTOMER));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.findByOrderId("customer-two", orderId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("A missing payment raises ResourceNotFoundException (404)")
    void missingPaymentIsNotFound() {
        signedInAs(newUser("admin", Role.ADMIN));
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.findById("admin", paymentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Payment not found");
    }
}
