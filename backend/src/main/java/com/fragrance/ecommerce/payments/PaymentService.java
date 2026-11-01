package com.fragrance.ecommerce.payments;

import com.fragrance.ecommerce.domain.Order;
import com.fragrance.ecommerce.domain.Payment;
import com.fragrance.ecommerce.domain.PaymentStatus;
import com.fragrance.ecommerce.domain.OrderStatus;
import com.fragrance.ecommerce.exception.NotFoundException;
import com.fragrance.ecommerce.repository.OrderRepository;
import com.fragrance.ecommerce.repository.PaymentRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final PaymentProvider provider;

    public PaymentService(OrderRepository orders, PaymentRepository payments, PaymentProvider provider) {
        this.orders = orders;
        this.payments = payments;
        this.provider = provider;
    }

    @Transactional
    public PaymentProvider.CheckoutSession checkout(UUID customerId, UUID orderId) {
        var order = ownedOrder(customerId, orderId);
        var payment = payments.findByOrderId(orderId)
            .orElseGet(() -> payments.save(new Payment(order, "stripe")));
        var session = provider.createCheckout(order, payment);
        payment.recordProviderPayment(session.providerPaymentId(), PaymentStatus.CREATED);
        return session;
    }

    @Transactional
    public void processWebhook(String payload, String signature) {
        var event = provider.verifyWebhook(payload, signature);
        if (event == null) return;
        var order = orders.findById(event.orderId())
            .orElseThrow(() -> new NotFoundException("Order was not found"));
        var payment = payments.findByOrderId(order.getId())
            .orElseThrow(() -> new NotFoundException("Payment was not found"));
        var status = switch (event.status()) {
            case PAID -> PaymentStatus.PAID;
            case FAILED -> PaymentStatus.FAILED;
            case CANCELED -> PaymentStatus.CANCELED;
        };
        payment.recordProviderPayment(event.providerPaymentId(), status);
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            switch (event.status()) {
                case PAID -> order.markPaid();
                case FAILED -> order.markPaymentFailed();
                case CANCELED -> order.markCanceled();
            }
        }
    }

    private Order ownedOrder(UUID customerId, UUID orderId) {
        return orders.findById(orderId)
            .filter(order -> order.getCustomer().getId().equals(customerId))
            .orElseThrow(() -> new NotFoundException("Order was not found"));
    }
}
