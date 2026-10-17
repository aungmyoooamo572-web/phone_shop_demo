package com.example.demo.payment.service;

import com.example.demo.catalog.dao.BrandRepository;
import com.example.demo.catalog.dao.CategoryRepository;
import com.example.demo.catalog.dao.PhoneRepository;
import com.example.demo.catalog.dao.PhoneVariantRepository;
import com.example.demo.catalog.entity.Brand;
import com.example.demo.catalog.entity.Category;
import com.example.demo.catalog.entity.Phone;
import com.example.demo.catalog.entity.PhoneVariant;
import com.example.demo.exception.ConflictException;
import com.example.demo.order.dao.OrderItemRepository;
import com.example.demo.order.dao.OrderRepository;
import com.example.demo.order.entity.Order;
import com.example.demo.order.entity.OrderItem;
import com.example.demo.order.entity.OrderStatus;
import com.example.demo.payment.dao.PaymentRepository;
import com.example.demo.payment.dto.CreatePaymentDto;
import com.example.demo.payment.dto.PaymentResponseDto;
import com.example.demo.payment.entity.Payment;
import com.example.demo.payment.entity.PaymentProvider;
import com.example.demo.payment.entity.PaymentStatus;
import com.example.demo.user.dao.UserRepository;
import com.example.demo.user.entity.Role;
import com.example.demo.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the stock guarantees of payment verification against the real
 * database, because the protection is a database row lock and cannot be
 * reproduced with mocks.
 *
 * <p>Each test creates its own throw-away brand, category, phone, variant,
 * orders and payments (all named with a {@code ZZ} prefix so they are easy to
 * spot) and removes them again in a {@code finally} block. Nothing that belongs
 * to the shop is touched.</p>
 */
@SpringBootTest
class PaymentStockConcurrencyTest {

    private static final int LOCK_TIMEOUT_SECONDS = 30;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PhoneVariantRepository phoneVariantRepository;

    @Autowired
    private PhoneRepository phoneRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private record Fixture(
            User user,
            Brand brand,
            Category category,
            Phone phone,
            PhoneVariant variant,
            List<Order> orders,
            List<Payment> payments
    ) {
    }

    // ------------------------------------------------------------- fixture

    private Fixture createFixture(int stock, int quantity, int orderCount) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User user = new User();
        user.setUsername("it_stock_" + suffix);
        user.setEmail("it_stock_" + suffix + "@example.com");
        user.setPassword("not-used-by-this-test");
        user.setRole(Role.CUSTOMER);
        user = userRepository.save(user);

        Brand brand = new Brand();
        brand.setName("ZZ Stock Test Brand " + suffix);
        brand = brandRepository.save(brand);

        Category category = new Category();
        category.setName("ZZ Stock Test Category " + suffix);
        category = categoryRepository.save(category);

        Phone phone = new Phone();
        phone.setName("ZZ Stock Test Phone " + suffix);
        phone.setModel("ZZ-" + suffix);
        phone.setBrand(brand);
        phone.setCategory(category);
        phone = phoneRepository.save(phone);

        PhoneVariant variant = new PhoneVariant();
        variant.setRam("8GB");
        variant.setStorage("128GB");
        variant.setColor("Black");
        variant.setPrice(new BigDecimal("100000.00"));
        variant.setStock(stock);
        variant.setSku("ZZ-STOCK-" + suffix);
        variant.setPhone(phone);
        variant = phoneVariantRepository.save(variant);

        List<Order> orders = new ArrayList<>();
        List<Payment> payments = new ArrayList<>();

        for (int i = 0; i < orderCount; i++) {
            Order order = new Order();
            order.setUser(user);
            order.setStatus(OrderStatus.PENDING);
            order.setShippingAddress("Integration Test");
            order.setTotalAmount(
                    variant.getPrice().multiply(BigDecimal.valueOf(quantity)));
            order = orderRepository.save(order);

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setPhoneVariant(variant);
            item.setProductName(phone.getName());
            item.setSku(variant.getSku());
            item.setPrice(variant.getPrice());
            item.setQuantity(quantity);
            orderItemRepository.save(item);

            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setAmount(order.getTotalAmount());
            payment.setProvider(PaymentProvider.KPAY);
            payment.setStatus(PaymentStatus.PENDING);
            payment = paymentRepository.save(payment);

            orders.add(order);
            payments.add(payment);
        }

        return new Fixture(user, brand, category, phone, variant, orders, payments);
    }

    private void deleteFixture(Fixture fixture) {
        // Delete every payment that points at these orders, not just the ones the
        // fixture created: several tests add a retry payment of their own.
        fixture.orders().forEach(order ->
                paymentRepository.findByOrderId(order.getId())
                        .forEach(payment ->
                                paymentRepository.deleteById(payment.getId())));
        fixture.orders()
                .forEach(order -> orderRepository.deleteById(order.getId()));
        // The phone cascades to its variants and images.
        phoneRepository.deleteById(fixture.phone().getId());
        brandRepository.deleteById(fixture.brand().getId());
        categoryRepository.deleteById(fixture.category().getId());
        userRepository.deleteById(fixture.user().getId());
    }

    private int stockOf(Fixture fixture) {
        return phoneVariantRepository.findById(fixture.variant().getId())
                .orElseThrow()
                .getStock();
    }

    private PaymentStatus statusOf(Payment payment) {
        return paymentRepository.findById(payment.getId())
                .orElseThrow()
                .getStatus();
    }

    private OrderStatus statusOf(Order order) {
        return orderRepository.findById(order.getId())
                .orElseThrow()
                .getStatus();
    }

    // ------------------------------------------------------------ plumbing

    /** Runs the verification and returns {@code null} on success, else the failure. */
    private Object verifyOutcome(UUID paymentId) {
        try {
            paymentService.verifyPayment(paymentId);
            return null;
        } catch (RuntimeException ex) {
            return ex;
        }
    }

    /** Runs the payment creation and returns {@code null} on success, else the failure. */
    private Object createOutcome(String username, UUID orderId, PaymentProvider provider) {
        CreatePaymentDto dto = new CreatePaymentDto();
        dto.setOrderId(orderId);
        dto.setProvider(provider);
        dto.setTransactionId("ZZ-ATTEMPT-" + UUID.randomUUID());

        try {
            paymentService.createPayment(username, dto);
            return null;
        } catch (RuntimeException ex) {
            return ex;
        }
    }

    /** Starts every task at the same moment so they really overlap. */
    private List<Object> runConcurrently(List<Callable<Object>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CyclicBarrier startTogether = new CyclicBarrier(tasks.size());

        try {
            List<Future<Object>> futures = new ArrayList<>();

            for (Callable<Object> task : tasks) {
                futures.add(pool.submit(() -> {
                    startTogether.await(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    return task.call();
                }));
            }

            List<Object> results = new ArrayList<>();

            for (Future<Object> future : futures) {
                results.add(future.get(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }

            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private Throwable onlyFailure(List<Object> results) {
        List<Object> failures = results.stream()
                .filter(Objects::nonNull)
                .toList();

        assertThat(failures)
                .as("exactly one of the concurrent verifications must fail")
                .hasSize(1);

        return (Throwable) failures.get(0);
    }

    // --------------------------------------------------------------- tests

    @Test
    @DisplayName("Two simultaneous payments for the last unit: one wins, stock never goes negative")
    void concurrentVerificationCannotOversell() throws Exception {
        Fixture fixture = createFixture(1, 1, 2);

        try {
            List<Object> results = runConcurrently(List.of(
                    () -> verifyOutcome(fixture.payments().get(0).getId()),
                    () -> verifyOutcome(fixture.payments().get(1).getId())
            ));

            assertThat(results.stream().filter(Objects::isNull).count())
                    .as("exactly one payment may be verified")
                    .isEqualTo(1);

            assertThat(onlyFailure(results))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Not enough stock");

            assertThat(stockOf(fixture))
                    .as("stock must be 0, never negative")
                    .isEqualTo(0);
        } finally {
            deleteFixture(fixture);
        }
    }

    @Test
    @DisplayName("Verifying the same payment twice at once deducts the stock exactly once")
    void concurrentDuplicateVerificationDeductsOnce() throws Exception {
        Fixture fixture = createFixture(5, 1, 1);
        UUID paymentId = fixture.payments().get(0).getId();

        try {
            List<Object> results = runConcurrently(List.of(
                    () -> verifyOutcome(paymentId),
                    () -> verifyOutcome(paymentId)
            ));

            assertThat(results.stream().filter(Objects::isNull).count())
                    .as("only one verification may succeed")
                    .isEqualTo(1);

            assertThat(onlyFailure(results))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Payment is already verified");

            assertThat(stockOf(fixture))
                    .as("5 - 1 = 4, deducted once only")
                    .isEqualTo(4);
        } finally {
            deleteFixture(fixture);
        }
    }

    @Test
    @DisplayName("Verifying the same payment twice in a row deducts the stock exactly once")
    void sequentialDuplicateVerificationDeductsOnce() {
        Fixture fixture = createFixture(5, 1, 1);
        UUID paymentId = fixture.payments().get(0).getId();

        try {
            paymentService.verifyPayment(paymentId);

            assertThatThrownBy(() -> paymentService.verifyPayment(paymentId))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Payment is already verified");

            assertThat(stockOf(fixture)).isEqualTo(4);
        } finally {
            deleteFixture(fixture);
        }
    }

    @Test
    @DisplayName("Insufficient stock rolls back: stock, payment and order stay untouched")
    void insufficientStockRollsEverythingBack() {
        Fixture fixture = createFixture(1, 2, 1);
        Payment payment = fixture.payments().get(0);
        Order order = fixture.orders().get(0);

        try {
            assertThatThrownBy(() -> paymentService.verifyPayment(payment.getId()))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Not enough stock for: " + fixture.variant().getSku());

            assertThat(stockOf(fixture)).as("stock must not change").isEqualTo(1);
            assertThat(statusOf(payment))
                    .as("the failed verification must not be persisted")
                    .isEqualTo(PaymentStatus.PENDING);
            assertThat(statusOf(order)).isEqualTo(OrderStatus.PENDING);
        } finally {
            deleteFixture(fixture);
        }
    }

    @Test
    @DisplayName("A successful verification deducts the stock and confirms the order")
    void successfulVerificationIsPersisted() {
        Fixture fixture = createFixture(3, 2, 1);
        Payment payment = fixture.payments().get(0);
        Order order = fixture.orders().get(0);

        try {
            paymentService.verifyPayment(payment.getId());

            assertThat(stockOf(fixture)).isEqualTo(1);
            assertThat(statusOf(payment)).isEqualTo(PaymentStatus.VERIFIED);
            assertThat(statusOf(order)).isEqualTo(OrderStatus.CONFIRMED);
        } finally {
            deleteFixture(fixture);
        }
    }

    @Test
    @DisplayName("A rejected payment can be retried and the retry can then be verified")
    void rejectedPaymentCanBeRetried() {
        Fixture fixture = createFixture(5, 2, 1);
        Payment rejected = fixture.payments().get(0);
        Order order = fixture.orders().get(0);
        UUID retryId = null;

        try {
            paymentService.rejectPayment(rejected.getId());

            assertThat(statusOf(rejected)).isEqualTo(PaymentStatus.REJECTED);
            assertThat(statusOf(order))
                    .as("a rejected payment must leave the order payable")
                    .isEqualTo(OrderStatus.PENDING);

            CreatePaymentDto retry = new CreatePaymentDto();
            retry.setOrderId(order.getId());
            retry.setProvider(PaymentProvider.WAVEPAY);
            retry.setTransactionId("ZZ-RETRY-" + order.getId());

            PaymentResponseDto created =
                    paymentService.createPayment(fixture.user().getUsername(), retry);
            retryId = created.getId();

            assertThat(created.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(retryId).isNotEqualTo(rejected.getId());
            assertThat(stockOf(fixture))
                    .as("submitting a payment must not touch the stock")
                    .isEqualTo(5);

            paymentService.verifyPayment(retryId);

            assertThat(statusOf(order)).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(stockOf(fixture))
                    .as("only the successful retry deducts the stock")
                    .isEqualTo(3);
        } finally {
            deleteFixture(fixture);
        }
    }

    @Test
    @DisplayName("Two simultaneous payment attempts for one order create exactly one pending payment")
    void concurrentPaymentCreationCreatesOnePendingPayment() throws Exception {
        Fixture fixture = createFixture(5, 1, 1);
        Order order = fixture.orders().get(0);
        String username = fixture.user().getUsername();

        try {
            // Clear the fixture's own payment first, so both attempts really race
            // on CREATE (and the rejection/retry path is exercised as well).
            paymentService.rejectPayment(fixture.payments().get(0).getId());

            List<Object> results = runConcurrently(List.of(
                    () -> createOutcome(username, order.getId(), PaymentProvider.KPAY),
                    () -> createOutcome(username, order.getId(), PaymentProvider.WAVEPAY)
            ));

            assertThat(results.stream().filter(Objects::isNull).count())
                    .as("only one of the two attempts may create a payment")
                    .isEqualTo(1);

            assertThat(onlyFailure(results))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Payment is already pending for this order");

            long pending = paymentRepository.findByOrderId(order.getId())
                    .stream()
                    .filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
                    .count();

            assertThat(pending)
                    .as("an order must never hold two pending payments")
                    .isEqualTo(1);

            assertThat(stockOf(fixture))
                    .as("creating a payment must never touch the stock")
                    .isEqualTo(5);
        } finally {
            deleteFixture(fixture);
        }
    }

    @Test
    @DisplayName("A rejected payment is kept in the history and only one retry may be pending")
    void rejectedPaymentHistoryIsKeptForRetry() {
        Fixture fixture = createFixture(5, 1, 1);
        Order order = fixture.orders().get(0);

        try {
            Payment first = fixture.payments().get(0);
            paymentService.rejectPayment(first.getId());

            // history: the rejected row is still there, untouched
            assertThat(paymentRepository.findById(first.getId()))
                    .as("a rejected payment must never be overwritten")
                    .isPresent();
            assertThat(statusOf(first)).isEqualTo(PaymentStatus.REJECTED);

            PaymentResponseDto retry = paymentService.createPayment(
                    fixture.user().getUsername(),
                    retryDto(order.getId())
            );

            assertThat(retry.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(paymentRepository.findByOrderId(order.getId()))
                    .as("both the rejected row and the new attempt are stored")
                    .hasSize(2);

            // ... and a third attempt while the retry is pending is refused
            assertThatThrownBy(() -> paymentService.createPayment(
                    fixture.user().getUsername(),
                    retryDto(order.getId())
            ))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Payment is already pending for this order");
        } finally {
            deleteFixture(fixture);
        }
    }

    @Test
    @DisplayName("A cancelled order cannot be paid")
    void cancelledOrderCannotBePaid() {
        Fixture fixture = createFixture(5, 1, 1);
        Order order = fixture.orders().get(0);

        try {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            assertThatThrownBy(() -> paymentService.createPayment(
                    fixture.user().getUsername(),
                    retryDto(order.getId())
            ))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("This order has been cancelled");

            assertThat(paymentRepository.findByOrderId(order.getId()))
                    .as("no payment may be added to a cancelled order")
                    .hasSize(1);
        } finally {
            deleteFixture(fixture);
        }
    }

    private CreatePaymentDto retryDto(UUID orderId) {
        CreatePaymentDto dto = new CreatePaymentDto();
        dto.setOrderId(orderId);
        dto.setProvider(PaymentProvider.WAVEPAY);
        dto.setTransactionId("ZZ-RETRY-" + UUID.randomUUID());
        return dto;
    }
}
