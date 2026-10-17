package com.example.demo.payment.service;

import com.example.demo.catalog.dao.PhoneVariantRepository;
import com.example.demo.catalog.entity.PhoneVariant;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.order.dao.OrderItemRepository;
import com.example.demo.order.dao.OrderRepository;
import com.example.demo.order.entity.Order;
import com.example.demo.order.entity.OrderStatus;
import com.example.demo.payment.dao.PaymentRepository;
import com.example.demo.payment.dto.CreatePaymentDto;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the stock rules of {@link PaymentService#verifyPayment(UUID)}:
 * no negative stock, no deduction without enough stock, no double deduction for
 * the same payment, and no double deduction for the same order.
 *
 * <p>The row locks that make this safe under concurrency live in the database,
 * so they are covered by {@code PaymentStockConcurrencyTest} instead.</p>
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceStockTest {

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

    private UUID variantId;

    private Order order;

    private Payment payment;

    private PhoneVariant variant;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        variantId = UUID.randomUUID();

        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setUsername("customer-one");
        owner.setRole(Role.CUSTOMER);

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

        variant = new PhoneVariant();
        variant.setId(variantId);
        variant.setSku("SKU-1");
        variant.setStock(10);
    }

    private void lockedPayment(PaymentStatus status) {
        payment.setStatus(status);
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
    }

    private void orderHolds(int quantity) {
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.VERIFIED))
                .thenReturn(false);
        when(orderItemRepository.findVariantQuantitiesByOrderId(orderId))
                .thenReturn(List.<Object[]>of(new Object[]{variantId, quantity}));
        when(phoneVariantRepository.findAllByIdForUpdate(anyCollection()))
                .thenReturn(List.of(variant));
    }

    @Test
    @DisplayName("A successful verification deducts the stock and confirms the order")
    void successfulVerificationDeductsStock() {
        lockedPayment(PaymentStatus.PENDING);
        orderHolds(3);
        when(paymentRepository.save(payment)).thenReturn(payment);

        PaymentResponseDto dto = paymentService.verifyPayment(paymentId);

        assertThat(variant.getStock()).isEqualTo(7);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.VERIFIED);
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(dto.getStatus()).isEqualTo(PaymentStatus.VERIFIED);
    }

    @Test
    @DisplayName("A not-yet-verified payment whose stock is insufficient is rejected and NOT saved")
    void insufficientStockLeavesThePaymentPending() {
        lockedPayment(PaymentStatus.PENDING);
        orderHolds(11); // only 10 in stock

        assertThatThrownBy(() -> paymentService.verifyPayment(paymentId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Not enough stock for: SKU-1");

        assertThat(variant.getStock()).isEqualTo(10);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Verifying the same payment twice is a conflict and never deducts twice")
    void duplicateVerificationDoesNotDeductAgain() {
        lockedPayment(PaymentStatus.VERIFIED);

        assertThatThrownBy(() -> paymentService.verifyPayment(paymentId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Payment is already verified");

        assertThat(variant.getStock()).isEqualTo(10);
        verify(paymentRepository, never()).save(any());
        verify(orderItemRepository, never()).findVariantQuantitiesByOrderId(any());
    }

    @Test
    @DisplayName("A rejected payment cannot be verified and never deducts stock")
    void rejectedPaymentCannotBeVerified() {
        lockedPayment(PaymentStatus.REJECTED);

        assertThatThrownBy(() -> paymentService.verifyPayment(paymentId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Rejected payment cannot be verified");

        assertThat(variant.getStock()).isEqualTo(10);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("An order that already has a verified payment is not charged twice")
    void orderAlreadyPaidIsNotDeductedAgain() {
        lockedPayment(PaymentStatus.PENDING);
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.VERIFIED))
                .thenReturn(true);

        assertThatThrownBy(() -> paymentService.verifyPayment(paymentId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("This order has already been paid");

        assertThat(variant.getStock()).isEqualTo(10);
        verify(paymentRepository, never()).save(any());
        verify(orderItemRepository, never()).findVariantQuantitiesByOrderId(any());
    }

    @Test
    @DisplayName("Quantities of repeated lines of the same variant are summed")
    void repeatedLinesAreSummed() {
        lockedPayment(PaymentStatus.PENDING);
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.VERIFIED))
                .thenReturn(false);
        when(orderItemRepository.findVariantQuantitiesByOrderId(orderId))
                .thenReturn(List.of(
                        new Object[]{variantId, 2},
                        new Object[]{variantId, 3}
                ));
        when(phoneVariantRepository.findAllByIdForUpdate(anyCollection()))
                .thenReturn(List.of(variant));

        // 2 + 3 = 5 needed, only 4 available -> refused
        variant.setStock(4);

        assertThatThrownBy(() -> paymentService.verifyPayment(paymentId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Not enough stock for: SKU-1");

        assertThat(variant.getStock()).isEqualTo(4);
    }

    @Test
    @DisplayName("A variant that is no longer active cannot be sold")
    void inactiveVariantIsRejected() {
        lockedPayment(PaymentStatus.PENDING);
        orderHolds(1);
        variant.setActive(false);

        assertThatThrownBy(() -> paymentService.verifyPayment(paymentId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Product is no longer available: SKU-1");

        assertThat(variant.getStock()).isEqualTo(10);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("An unknown payment id raises ResourceNotFoundException (404)")
    void missingPaymentIsNotFound() {
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.verifyPayment(paymentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Payment not found");
    }

    @Test
    @DisplayName("An order without items verifies successfully and touches no stock")
    void orderWithoutItemsDoesNotTouchStock() {
        lockedPayment(PaymentStatus.PENDING);
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.VERIFIED))
                .thenReturn(false);
        when(orderItemRepository.findVariantQuantitiesByOrderId(orderId))
                .thenReturn(List.of());
        when(paymentRepository.save(payment)).thenReturn(payment);

        paymentService.verifyPayment(paymentId);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.VERIFIED);
        assertThat(variant.getStock()).isEqualTo(10);
        verify(phoneVariantRepository, never()).findAllByIdForUpdate(anyCollection());
    }

    @Test
    @DisplayName("Creating a second payment for an already paid order is refused")
    void secondPaymentForAnAlreadyPaidOrderIsRefused() {
        when(userService.findByUsername("customer-one")).thenReturn(Optional.of(order.getUser()));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.VERIFIED))
                .thenReturn(true);

        CreatePaymentDto dto = new CreatePaymentDto();
        dto.setOrderId(orderId);
        dto.setProvider(PaymentProvider.KPAY);

        assertThatThrownBy(() -> paymentService.createPayment("customer-one", dto))
                .isInstanceOf(ConflictException.class)
                .hasMessage("This order has already been paid");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Creating a second pending payment for the same order is refused")
    void secondPendingPaymentIsRefused() {
        when(userService.findByUsername("customer-one")).thenReturn(Optional.of(order.getUser()));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.VERIFIED))
                .thenReturn(false);
        when(paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.PENDING))
                .thenReturn(true);

        CreatePaymentDto dto = new CreatePaymentDto();
        dto.setOrderId(orderId);
        dto.setProvider(PaymentProvider.KPAY);

        assertThatThrownBy(() -> paymentService.createPayment("customer-one", dto))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Payment is already pending for this order");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("A rejected payment does not block a new payment for the same order")
    void aRejectedPaymentDoesNotBlockANewPayment() {
        // The two guards only look at VERIFIED and PENDING, so a REJECTED payment
        // leaves the order payable and the customer can submit a fresh proof.
        when(userService.findByUsername("customer-one")).thenReturn(Optional.of(order.getUser()));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.VERIFIED))
                .thenReturn(false);
        when(paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.PENDING))
                .thenReturn(false);
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreatePaymentDto retry = new CreatePaymentDto();
        retry.setOrderId(orderId);
        retry.setProvider(PaymentProvider.WAVEPAY);
        retry.setTransactionId("RETRY-1");

        PaymentResponseDto created =
                paymentService.createPayment("customer-one", retry);

        assertThat(created.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(created.getOrderId()).isEqualTo(orderId);
        assertThat(created.getProvider()).isEqualTo(PaymentProvider.WAVEPAY);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("A cancelled order cannot be paid")
    void cancelledOrderCannotBePaid() {
        when(userService.findByUsername("customer-one")).thenReturn(Optional.of(order.getUser()));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        order.setStatus(OrderStatus.CANCELLED);

        CreatePaymentDto dto = new CreatePaymentDto();
        dto.setOrderId(orderId);
        dto.setProvider(PaymentProvider.KPAY);

        assertThatThrownBy(() -> paymentService.createPayment("customer-one", dto))
                .isInstanceOf(ConflictException.class)
                .hasMessage("This order has been cancelled");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("A payment creation for an order owned by somebody else is forbidden")
    void paymentForAnotherCustomersOrderIsForbidden() {
        User otherCustomer = new User();
        otherCustomer.setId(UUID.randomUUID());
        otherCustomer.setUsername("customer-two");
        otherCustomer.setRole(Role.CUSTOMER);

        when(userService.findByUsername("customer-two")).thenReturn(Optional.of(otherCustomer));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        CreatePaymentDto dto = new CreatePaymentDto();
        dto.setOrderId(orderId);
        dto.setProvider(PaymentProvider.KPAY);

        assertThatThrownBy(() -> paymentService.createPayment("customer-two", dto))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessage("You are not allowed to make payment for this order");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rejecting a payment keeps the row and lets the customer retry")
    void rejectingKeepsTheRowForRetry() {
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        PaymentResponseDto rejected = paymentService.rejectPayment(paymentId);

        assertThat(rejected.getStatus()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(rejected.getId()).isEqualTo(paymentId);
        // The order is untouched, so a new payment attempt is still possible.
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }
}
