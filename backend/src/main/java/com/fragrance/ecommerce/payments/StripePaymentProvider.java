package com.fragrance.ecommerce.payments;

import com.fragrance.ecommerce.configuration.StripeProperties;
import com.fragrance.ecommerce.domain.Order;
import com.fragrance.ecommerce.domain.Payment;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StripePaymentProvider implements PaymentProvider {
    private final StripeProperties properties;

    public StripePaymentProvider(StripeProperties properties) {
        this.properties = properties;
    }

    @Override
    public CheckoutSession createCheckout(Order order, Payment payment) {
        requireConfigured(properties.secretKey(), "secret key");
        Stripe.apiKey = properties.secretKey();
        try {
            var builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(properties.successUrl())
                .setCancelUrl(properties.cancelUrl())
                .putMetadata("order_id", order.getId().toString())
                .addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(order.getCurrency().toLowerCase())
                        .setUnitAmount(order.getTotal().movePointRight(2).longValueExact())
                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName("Fragrance order " + order.getId()).build())
                        .build())
                    .build());
            Session session = Session.create(builder.build());
            return new CheckoutSession(session.getId(), session.getUrl());
        } catch (StripeException | ArithmeticException exception) {
            throw new IllegalStateException("Stripe checkout could not be created", exception);
        }
    }

    @Override
    public VerifiedPayment verifyWebhook(String payload, String signature) {
        requireConfigured(properties.webhookSecret(), "webhook secret");
        try {
            Event event = Webhook.constructEvent(payload, signature, properties.webhookSecret());
            var status = switch (event.getType()) {
                case "checkout.session.completed" -> EventStatus.PAID;
                case "checkout.session.async_payment_failed" -> EventStatus.FAILED;
                case "checkout.session.expired" -> EventStatus.CANCELED;
                default -> null;
            };
            if (status == null) return null;
            StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
            if (!(object instanceof Session session)) {
                throw new IllegalArgumentException("Stripe webhook payload is invalid");
            }
            String orderId = session.getMetadata().get("order_id");
            if (orderId == null) throw new IllegalArgumentException("Stripe webhook order is missing");
            return new VerifiedPayment(UUID.fromString(orderId), session.getId(), status);
        } catch (Exception exception) {
            if (exception instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new IllegalArgumentException("Stripe webhook signature is invalid", exception);
        }
    }

    private void requireConfigured(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Stripe " + name + " is not configured");
        }
    }
}
