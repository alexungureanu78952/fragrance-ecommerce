package com.fragrance.ecommerce.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fragrance.ecommerce.domain.Brand;
import com.fragrance.ecommerce.domain.Cart;
import com.fragrance.ecommerce.domain.Category;
import com.fragrance.ecommerce.domain.Customer;
import com.fragrance.ecommerce.domain.Order;
import com.fragrance.ecommerce.domain.Payment;
import com.fragrance.ecommerce.domain.PaymentStatus;
import com.fragrance.ecommerce.domain.Product;
import com.fragrance.ecommerce.domain.Role;
import com.fragrance.ecommerce.exception.CheckoutUnavailableException;
import com.fragrance.ecommerce.exception.ConflictException;
import com.fragrance.ecommerce.repository.CartRepository;
import com.fragrance.ecommerce.repository.OrderRepository;
import com.fragrance.ecommerce.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock OrderRepository orders;
    @Mock PaymentRepository payments;
    @Mock PaymentProvider provider;
    @Mock CartRepository carts;
    private PaymentService service;
    private Customer customer;
    private Order order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        service = new PaymentService(orders, payments, provider, carts);
        customer = new Customer("customer@example.test", "hash", "Customer",
            new Role(UUID.randomUUID(), "CUSTOMER"));
        order = new Order(customer, "order-key", BigDecimal.TEN);
        payment = new Payment(order, "stripe");
        lenient().when(orders.findById(order.getId())).thenReturn(Optional.of(order));
        lenient().when(payments.findByOrderId(order.getId())).thenReturn(Optional.of(payment));
    }

    @Test
    void checkoutCreatesProviderSessionForOwnedOrder() {
        when(provider.createCheckout(order, payment))
            .thenReturn(new PaymentProvider.CheckoutSession("cs_test_123", "https://checkout.stripe.test"));

        var session = service.checkout(customer.getId(), order.getId());

        assertThat(session.checkoutUrl()).isEqualTo("https://checkout.stripe.test");
        assertThat(payment.getProviderPaymentId()).isEqualTo("cs_test_123");
        verify(provider).createCheckout(order, payment);
    }

    @Test
    void paidWebhookUpdatesPaymentAndOrder() {
        when(provider.verifyWebhook("payload", "signature"))
            .thenReturn(new PaymentProvider.VerifiedPayment(
                order.getId(), "cs_test_123", PaymentProvider.EventStatus.PAID));

        service.processWebhook("payload", "signature");

        assertThat(payment.getStatus().name()).isEqualTo("PAID");
        assertThat(order.getStatus().name()).isEqualTo("PAID");
    }

    @Test
    void providerFailureMarksOrderAndPaymentFailed() {
        when(provider.createCheckout(order, payment))
            .thenThrow(new IllegalStateException("Stripe secret key is missing"));

        assertThatThrownBy(() -> service.checkout(customer.getId(), order.getId()))
            .isInstanceOf(CheckoutUnavailableException.class)
            .hasMessageContaining("No payment was taken");

        assertThat(payment.getStatus().name()).isEqualTo("FAILED");
        assertThat(order.getStatus().name()).isEqualTo("PAYMENT_FAILED");
    }

    @Test
    void unknownWebhookEventDoesNotChangeOrder() {
        when(provider.verifyWebhook("payload", "signature")).thenReturn(null);

        service.processWebhook("payload", "signature");

        assertThat(order.getStatus().name()).isEqualTo("PENDING_PAYMENT");
    }

    @Test
    void customerCancelExpiresCheckoutReleasesStockAndLeavesCartUnchanged() {
        var product = new Product(new Brand("Brand", "d"), new Category("Woody", "d"), "Dusk", "d",
            new BigDecimal("42.00"), 5);
        var pendingOrder = new Order(customer, "cancel-key", new BigDecimal("84.00"));
        product.decreaseStock(2);
        pendingOrder.addItem(product, 2);
        var pendingPayment = new Payment(pendingOrder, "stripe");
        pendingPayment.recordProviderPayment("cs_test_open", PaymentStatus.CREATED);
        var cart = new Cart(customer);
        cart.addItem(product, 2);
        when(orders.findById(pendingOrder.getId())).thenReturn(Optional.of(pendingOrder));
        when(payments.findByOrderId(pendingOrder.getId())).thenReturn(Optional.of(pendingPayment));
        lenient().when(carts.findByCustomerId(customer.getId())).thenReturn(Optional.of(cart));
        when(provider.checkoutState("cs_test_open")).thenReturn(PaymentProvider.CheckoutState.OPEN);

        var canceled = service.cancelPending(customer.getId(), pendingOrder.getId());

        verify(provider).expireCheckout("cs_test_open");
        assertThat(canceled.getStatus().name()).isEqualTo("CANCELED");
        assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(product.getStockQuantity()).isEqualTo(5);
        assertThat(cart.getItems()).singleElement()
            .satisfies(item -> assertThat(item.getQuantity()).isEqualTo(2));
    }

    @Test
    void customerCannotCancelPaidOrder() {
        order.markPaid();

        assertThatThrownBy(() -> service.cancelPending(customer.getId(), order.getId()))
            .isInstanceOf(ConflictException.class);
        verify(provider, never()).expireCheckout(any());
    }

    @Test
    void cancelLeavesOrderUnchangedWhenProviderIsUnavailable() {
        payment.recordProviderPayment("cs_test_open", PaymentStatus.CREATED);
        when(provider.checkoutState("cs_test_open")).thenThrow(new IllegalStateException("Stripe down"));

        assertThatThrownBy(() -> service.cancelPending(customer.getId(), order.getId()))
            .isInstanceOf(CheckoutUnavailableException.class);
        assertThat(order.getStatus().name()).isEqualTo("PENDING_PAYMENT");
    }

    @Test
    void retryingCheckoutExpiresThePreviousSession() {
        payment.recordProviderPayment("cs_test_old", PaymentStatus.CREATED);
        when(provider.checkoutState("cs_test_old")).thenReturn(PaymentProvider.CheckoutState.OPEN);
        when(provider.createCheckout(order, payment))
            .thenReturn(new PaymentProvider.CheckoutSession("cs_test_new", "https://checkout.stripe.test/new"));

        service.checkout(customer.getId(), order.getId());

        verify(provider).expireCheckout("cs_test_old");
        assertThat(payment.getProviderPaymentId()).isEqualTo("cs_test_new");
    }

    @Test
    void cancelingAnOrderThatStripeAlreadyChargedMarksItPaid() {
        payment.recordProviderPayment("cs_test_paid", PaymentStatus.CREATED);
        when(provider.checkoutState("cs_test_paid")).thenReturn(PaymentProvider.CheckoutState.PAID);

        assertThatThrownBy(() -> service.cancelPending(customer.getId(), order.getId()))
            .isInstanceOf(ConflictException.class)
            .hasMessage("This order has already been paid");
        assertThat(order.getStatus().name()).isEqualTo("PAID");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(provider, never()).expireCheckout(any());
    }

    @Test
    void syncMarksOrderPaidWhenWebhookWasMissed() {
        payment.recordProviderPayment("cs_test_paid", PaymentStatus.CREATED);
        when(provider.checkoutState("cs_test_paid")).thenReturn(PaymentProvider.CheckoutState.PAID);

        var synced = service.sync(customer.getId(), order.getId());

        assertThat(synced.getStatus().name()).isEqualTo("PAID");
    }

    @Test
    void webhookForSupersededCheckoutIsIgnored() {
        payment.recordProviderPayment("cs_test_current", PaymentStatus.CREATED);
        when(provider.verifyWebhook("payload", "signature"))
            .thenReturn(new PaymentProvider.VerifiedPayment(
                order.getId(), "cs_test_old", PaymentProvider.EventStatus.CANCELED));

        service.processWebhook("payload", "signature");

        assertThat(order.getStatus().name()).isEqualTo("PENDING_PAYMENT");
        assertThat(payment.getProviderPaymentId()).isEqualTo("cs_test_current");
    }

    @Test
    void paymentRemovesThePurchasedItemsFromTheCart() {
        var product = new Product(new Brand("Brand", "d"), new Category("Woody", "d"), "Dusk", "d",
            new BigDecimal("42.00"), 5);
        var paidOrder = new Order(customer, "paid-key", new BigDecimal("42.00"));
        paidOrder.addItem(product, 1);
        var paidPayment = new Payment(paidOrder, "stripe");
        var cart = new Cart(customer);
        cart.addItem(product, 3);
        when(orders.findById(paidOrder.getId())).thenReturn(Optional.of(paidOrder));
        when(payments.findByOrderId(paidOrder.getId())).thenReturn(Optional.of(paidPayment));
        when(carts.findByCustomerId(customer.getId())).thenReturn(Optional.of(cart));
        when(provider.verifyWebhook("payload", "signature"))
            .thenReturn(new PaymentProvider.VerifiedPayment(paidOrder.getId(), "cs_test_paid", PaymentProvider.EventStatus.PAID));

        service.processWebhook("payload", "signature");

        assertThat(paidOrder.getStatus().name()).isEqualTo("PAID");
        assertThat(cart.getItems()).singleElement()
            .satisfies(item -> assertThat(item.getQuantity()).isEqualTo(2));
    }

    @Test
    void startingANewCheckoutAbandonsTheUnpaidOne() {
        payment.recordProviderPayment("cs_test_left", PaymentStatus.CREATED);
        when(orders.findByCustomerIdAndStatus(customer.getId(), com.fragrance.ecommerce.domain.OrderStatus.PENDING_PAYMENT))
            .thenReturn(java.util.List.of(order));
        when(provider.checkoutState("cs_test_left")).thenReturn(PaymentProvider.CheckoutState.OPEN);

        service.abandonPendingOrders(customer.getId());

        verify(provider).expireCheckout("cs_test_left");
        assertThat(order.getStatus().name()).isEqualTo("CANCELED");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
    }
}
