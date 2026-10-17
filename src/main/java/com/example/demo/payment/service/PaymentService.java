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
import com.example.demo.payment.entity.PaymentStatus;
import com.example.demo.user.entity.Role;
import com.example.demo.user.entity.User;
import com.example.demo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PhoneVariantRepository phoneVariantRepository;
    private final UserService userService;

    /**
     * Creates a PENDING payment for one of the caller's own orders.
     *
     * <p>Lifecycle rules (all HTTP 409): an order that was already paid cannot be
     * paid again, an order that already has a PENDING payment cannot collect a
     * second one, and a cancelled order is not payable. A REJECTED payment is
     * deliberately NOT one of those states: it stays in the history and the
     * customer may submit a fresh attempt.</p>
     *
     * <p>Concurrency: the order row is locked with the very first statement, so
     * two simultaneous attempts for the same order are serialised. Because that
     * locking read also establishes this transaction's read view after the winner
     * committed, the PENDING check below really sees the winner's row and one
     * order can never end up with two PENDING payments.</p>
     */
    public PaymentResponseDto createPayment(
            String username,
            CreatePaymentDto dto
    ) {
        // IMPORTANT: this must stay the first database access in the method.
        // A consistent read issued before it would freeze a REPEATABLE READ
        // snapshot that predates the competing transaction's commit, and the
        // duplicate-PENDING guard would silently read stale data.
        Order order = orderRepository.findByIdForUpdate(dto.getOrderId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"));

        User user = userService.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        // Creating a payment stays customer-only: an ADMIN may VIEW any payment,
        // but must not be able to pay on behalf of another customer.
        if (!order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                    "You are not allowed to make payment for this order"
            );
        }

        // A cancelled order is not payable. A REJECTED payment, by contrast, does
        // NOT cancel the order: the customer is free to submit a new attempt.
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("This order has been cancelled");
        }

        // The order was already paid: a second payment would deduct stock twice.
        if (paymentRepository.existsByOrderIdAndStatus(
                order.getId(),
                PaymentStatus.VERIFIED
        )) {
            throw new ConflictException("This order has already been paid");
        }

        if (paymentRepository.existsByOrderIdAndStatus(
                order.getId(),
                PaymentStatus.PENDING
        )) {
            throw new ConflictException(
                    "Payment is already pending for this order"
            );
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setProvider(dto.getProvider());
        payment.setTransactionId(dto.getTransactionId());
        payment.setPaymentSlipUrl(dto.getPaymentSlipUrl());
        payment.setStatus(PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.save(payment);

        return toDto(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> findByOrderId(
            String username,
            UUID orderId
    ) {
        User user = userService.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"));

        if (!canAccessOrder(user, order)) {
            throw new AccessDeniedException(
                    "You are not allowed to make payment for this order"
            );
        }

        return paymentRepository
                .findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponseDto findById(
            String username,
            UUID paymentId
    ) {
        User user = userService.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found"));

        if (!canAccessPayment(user, payment)) {
            throw new AccessDeniedException(
                    "You are not allowed to make payment for this order"
            );
        }

        return toDto(payment);
    }

    /**
     * Marks a PENDING payment as VERIFIED and reduces the stock of every ordered
     * variant. This is the only place that changes stock, and it has to survive
     * two race conditions:
     *
     * <ol>
     *   <li>the same payment verified twice -&gt; the payment row is locked, so the
     *       second request waits and then sees {@code VERIFIED};</li>
     *   <li>two payments competing for the last item(s) -&gt; the variant rows are
     *       locked, so the stock check and the deduction cannot interleave.</li>
     * </ol>
     *
     * The locks are always taken in the same order (payment, order, variants
     * sorted by id) to rule out deadlocks. The whole method runs inside the
     * class-level transaction, so an insufficient-stock failure rolls back the
     * status change as well: the payment is never left marked as verified.
     */
    public PaymentResponseDto verifyPayment(UUID paymentId) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.VERIFIED) {
            throw new ConflictException("Payment is already verified");
        }

        if (payment.getStatus() == PaymentStatus.REJECTED) {
            throw new ConflictException("Rejected payment cannot be verified");
        }

        Order order = orderRepository
                .findByIdForUpdate(payment.getOrder().getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"));

        // Another payment of the same order may already have deducted the stock.
        if (paymentRepository.existsByOrderIdAndStatus(
                order.getId(),
                PaymentStatus.VERIFIED
        )) {
            throw new ConflictException("This order has already been paid");
        }

        deductStock(order);

        payment.setStatus(PaymentStatus.VERIFIED);
        payment.setPaidAt(LocalDateTime.now());

        order.setStatus(OrderStatus.CONFIRMED);

        Payment savedPayment = paymentRepository.save(payment);

        return toDto(savedPayment);
    }

    /**
     * Verifies that every ordered variant is still sellable and has enough stock,
     * then subtracts the ordered quantities.
     *
     * <p>The variant rows are read with an exclusive write lock (ordered by id),
     * so the quantities used by the check are the committed ones and no other
     * transaction can change them until this one commits. Stock therefore can
     * never become negative, even when several payments are verified at the same
     * moment.</p>
     */
    private void deductStock(Order order) {
        // variantId -> total quantity needed, sorted by id for a stable lock order
        Map<UUID, Integer> required = new TreeMap<>();

        for (Object[] row : orderItemRepository
                .findVariantQuantitiesByOrderId(order.getId())) {
            required.merge(
                    (UUID) row[0],
                    ((Number) row[1]).intValue(),
                    Integer::sum
            );
        }

        if (required.isEmpty()) {
            return;
        }

        List<UUID> variantIds = new ArrayList<>(required.keySet());

        Map<UUID, PhoneVariant> lockedVariants = phoneVariantRepository
                .findAllByIdForUpdate(variantIds)
                .stream()
                .collect(Collectors.toMap(PhoneVariant::getId, variant -> variant));

        for (UUID variantId : variantIds) {
            PhoneVariant variant = lockedVariants.get(variantId);

            if (variant == null) {
                throw new ResourceNotFoundException(
                        "Phone variant not found: " + variantId
                );
            }

            if (!variant.isActive() || variant.isDeleted()) {
                throw new ConflictException(
                        "Product is no longer available: " + variant.getSku()
                );
            }

            if (variant.getStock() < required.get(variantId)) {
                throw new ConflictException(
                        "Not enough stock for: " + variant.getSku()
                );
            }
        }

        for (UUID variantId : variantIds) {
            PhoneVariant variant = lockedVariants.get(variantId);
            variant.setStock(variant.getStock() - required.get(variantId));
        }
    }

    /**
     * Rejects a PENDING payment. The row is read with a write lock for the same
     * reason {@link #verifyPayment(UUID)} does: an admin rejecting a payment at
     * the same moment another admin verifies it must not be able to overwrite the
     * final state (and vice versa). The rejected row is kept, never overwritten,
     * so the customer's payment history stays intact.
     */
    public PaymentResponseDto rejectPayment(UUID paymentId) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.VERIFIED) {
            throw new ConflictException("Verified payment cannot be rejected");
        }

        if (payment.getStatus() == PaymentStatus.REJECTED) {
            throw new ConflictException("Payment is already rejected");
        }

        payment.setStatus(PaymentStatus.REJECTED);

        Payment savedPayment = paymentRepository.save(payment);

        return toDto(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> findPendingPayments() {
        return paymentRepository.findByStatusOrderByCreatedAtDesc(PaymentStatus.PENDING)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> findAllPayments() {
        return paymentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    /*
     * ADMIN accounts may read any order/payment; a CUSTOMER may only read their own.
     */
    private boolean canAccessOrder(User user, Order order) {
        return user.getRole() == Role.ADMIN
                || order.getUser().getId().equals(user.getId());
    }

    private boolean canAccessPayment(User user, Payment payment) {
        return canAccessOrder(user, payment.getOrder());
    }

    private PaymentResponseDto toDto(Payment payment) {
        return new PaymentResponseDto(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getAmount(),
                payment.getProvider(),
                payment.getTransactionId(),
                payment.getPaymentSlipUrl(),
                payment.getStatus(),
                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }

}
