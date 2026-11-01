package com.fragrance.ecommerce.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.fragrance.ecommerce.domain.Brand;
import com.fragrance.ecommerce.domain.Category;
import com.fragrance.ecommerce.domain.Customer;
import com.fragrance.ecommerce.domain.Order;
import com.fragrance.ecommerce.domain.Payment;
import com.fragrance.ecommerce.domain.Role;
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
    private PaymentService service;
    private Customer customer;
    private Order order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        service = new PaymentService(orders, payments, provider);
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
    void unknownWebhookEventDoesNotChangeOrder() {
        when(provider.verifyWebhook("payload", "signature")).thenReturn(null);

        service.processWebhook("payload", "signature");

        assertThat(order.getStatus().name()).isEqualTo("PENDING_PAYMENT");
    }
}
