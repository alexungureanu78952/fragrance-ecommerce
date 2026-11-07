package com.fragrance.ecommerce.payments;

import com.fragrance.ecommerce.domain.Order;
import com.fragrance.ecommerce.domain.Payment;
import java.util.UUID;

public interface PaymentProvider {
    CheckoutSession createCheckout(Order order, Payment payment);
    VerifiedPayment verifyWebhook(String payload, String signature);

    /** Current state of a checkout at the provider, used to reconcile missed webhooks. */
    CheckoutState checkoutState(String providerPaymentId);

    /** Closes an open checkout so it can no longer be paid. */
    void expireCheckout(String providerPaymentId);

    record CheckoutSession(String providerPaymentId, String checkoutUrl) {}
    record VerifiedPayment(UUID orderId, String providerPaymentId, EventStatus status) {}
    enum EventStatus { PAID, FAILED, CANCELED }
    enum CheckoutState { OPEN, PAID, EXPIRED }
}
