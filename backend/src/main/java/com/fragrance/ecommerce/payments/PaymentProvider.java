package com.fragrance.ecommerce.payments;

import com.fragrance.ecommerce.domain.Order;
import com.fragrance.ecommerce.domain.Payment;
import java.util.UUID;

public interface PaymentProvider {
    CheckoutSession createCheckout(Order order, Payment payment);
    VerifiedPayment verifyWebhook(String payload, String signature);

    record CheckoutSession(String providerPaymentId, String checkoutUrl) {}
    record VerifiedPayment(UUID orderId, String providerPaymentId, EventStatus status) {}
    enum EventStatus { PAID, FAILED, CANCELED }
}
