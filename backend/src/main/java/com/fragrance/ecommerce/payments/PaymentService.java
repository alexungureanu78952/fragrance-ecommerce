package com.fragrance.ecommerce.payments;

import com.fragrance.ecommerce.domain.Order;
import com.fragrance.ecommerce.domain.Payment;
import com.fragrance.ecommerce.domain.PaymentStatus;
import com.fragrance.ecommerce.domain.OrderStatus;
import com.fragrance.ecommerce.exception.ConflictException;
import com.fragrance.ecommerce.exception.NotFoundException;
import com.fragrance.ecommerce.exception.CheckoutUnavailableException;
import com.fragrance.ecommerce.repository.CartRepository;
import com.fragrance.ecommerce.repository.OrderRepository;
import com.fragrance.ecommerce.repository.PaymentRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    static final String ALREADY_PAID = "This order has already been paid";

    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final PaymentProvider provider;
    private final CartRepository carts;

    public PaymentService(OrderRepository orders, PaymentRepository payments, PaymentProvider provider,
                          CartRepository carts) {
        this.orders = orders;
        this.payments = payments;
        this.provider = provider;
        this.carts = carts;
    }

    /**
     * Starts (or restarts) payment for a pending order. A previous open checkout is
     * expired first; if it was in fact paid, the order is marked paid instead.
     */
    @Transactional(noRollbackFor = {CheckoutUnavailableException.class, ConflictException.class})
    public PaymentProvider.CheckoutSession checkout(UUID customerId, UUID orderId) {
        var order = ownedOrder(customerId, orderId);
        requirePending(order, "Only orders awaiting payment can be paid");
        var payment = payments.findByOrderId(orderId)
            .orElseGet(() -> payments.save(new Payment(order, "stripe")));
        if (closeOpenCheckout(order, payment)) throw new ConflictException(ALREADY_PAID);
        PaymentProvider.CheckoutSession session;
        try {
            session = provider.createCheckout(order, payment);
        } catch (RuntimeException exception) {
            payment.recordProviderPayment(null, PaymentStatus.FAILED);
            releaseStock(order);
            order.markPaymentFailed();
            throw new CheckoutUnavailableException(
                "Payment checkout is temporarily unavailable. No payment was taken.", exception);
        }
        payment.recordProviderPayment(session.providerPaymentId(), PaymentStatus.CREATED);
        return session;
    }

    /**
     * Customer left checkout without paying: closes the provider checkout, cancels the
     * order, and releases the reserved stock. The cart was never changed, so it is
     * exactly as the customer left it.
     */
    @Transactional(noRollbackFor = ConflictException.class)
    public Order cancelPending(UUID customerId, UUID orderId) {
        var order = ownedOrder(customerId, orderId);
        requirePending(order, "Only orders awaiting payment can be canceled");
        if (abandon(order)) throw new ConflictException(ALREADY_PAID);
        return order;
    }

    /**
     * Closes every unpaid checkout of a customer before a new one starts, so stock is
     * never reserved twice. A checkout that was in fact paid is recorded as paid.
     */
    @Transactional
    public void abandonPendingOrders(UUID customerId) {
        for (var order : orders.findByCustomerIdAndStatus(customerId, OrderStatus.PENDING_PAYMENT)) {
            abandon(order);
        }
    }

    /** Cancels a pending order and releases its stock; returns true if it was actually paid. */
    private boolean abandon(Order order) {
        var payment = payments.findByOrderId(order.getId()).orElse(null);
        if (payment != null) {
            if (closeOpenCheckout(order, payment)) return true;
            payment.recordProviderPayment(payment.getProviderPaymentId(), PaymentStatus.CANCELED);
        }
        releaseStock(order);
        order.markCanceled();
        return false;
    }

    /**
     * Reconciles a pending order with the payment provider, so a missed or delayed
     * webhook cannot leave a paid order stuck in PENDING_PAYMENT. If the provider is
     * unreachable the order is returned unchanged.
     */
    @Transactional
    public Order sync(UUID customerId, UUID orderId) {
        var order = ownedOrder(customerId, orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) return order;
        var payment = payments.findByOrderId(orderId).orElse(null);
        if (payment == null || payment.getProviderPaymentId() == null
            || payment.getStatus() != PaymentStatus.CREATED) {
            return order;
        }
        PaymentProvider.CheckoutState state;
        try {
            state = provider.checkoutState(payment.getProviderPaymentId());
        } catch (RuntimeException exception) {
            return order;
        }
        switch (state) {
            case PAID -> markPaid(order, payment, payment.getProviderPaymentId());
            case EXPIRED -> {
                payment.recordProviderPayment(payment.getProviderPaymentId(), PaymentStatus.CANCELED);
                releaseStock(order);
                order.markCanceled();
            }
            case OPEN -> { /* still awaiting payment */ }
        }
        return order;
    }

    @Transactional
    public void processWebhook(String payload, String signature) {
        var event = provider.verifyWebhook(payload, signature);
        if (event == null) return;
        var order = orders.findById(event.orderId())
            .orElseThrow(() -> new NotFoundException("Order was not found"));
        var payment = payments.findByOrderId(order.getId())
            .orElseThrow(() -> new NotFoundException("Payment was not found"));
        // Events for a superseded checkout (e.g. the one expired by a payment retry)
        // must not change the order.
        if (payment.getProviderPaymentId() != null
            && !payment.getProviderPaymentId().equals(event.providerPaymentId())) {
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            if (event.status() == PaymentProvider.EventStatus.PAID) {
                payment.recordProviderPayment(event.providerPaymentId(), PaymentStatus.PAID);
            }
            return;
        }
        switch (event.status()) {
            case PAID -> markPaid(order, payment, event.providerPaymentId());
            case FAILED -> {
                payment.recordProviderPayment(event.providerPaymentId(), PaymentStatus.FAILED);
                releaseStock(order);
                order.markPaymentFailed();
            }
            case CANCELED -> {
                payment.recordProviderPayment(event.providerPaymentId(), PaymentStatus.CANCELED);
                releaseStock(order);
                order.markCanceled();
            }
        }
    }

    /**
     * Ensures no payable checkout remains open. Returns true if the open checkout had
     * actually been paid, in which case the order is marked paid.
     */
    private boolean closeOpenCheckout(Order order, Payment payment) {
        if (payment.getProviderPaymentId() == null || payment.getStatus() != PaymentStatus.CREATED) return false;
        PaymentProvider.CheckoutState state;
        try {
            state = provider.checkoutState(payment.getProviderPaymentId());
            if (state == PaymentProvider.CheckoutState.OPEN) {
                provider.expireCheckout(payment.getProviderPaymentId());
            }
        } catch (RuntimeException exception) {
            throw new CheckoutUnavailableException(
                "The payment provider is unavailable, so the order was not changed. Try again shortly.", exception);
        }
        if (state == PaymentProvider.CheckoutState.PAID) {
            markPaid(order, payment, payment.getProviderPaymentId());
            return true;
        }
        return false;
    }

    /** Records the payment and removes the purchased items from the customer's cart. */
    private void markPaid(Order order, Payment payment, String providerPaymentId) {
        payment.recordProviderPayment(providerPaymentId, PaymentStatus.PAID);
        order.markPaid();
        carts.findByCustomerId(order.getCustomer().getId()).ifPresent(cart -> {
            for (var item : order.getItems()) {
                if (item.getProduct() != null) cart.removeQuantity(item.getProduct().getId(), item.getQuantity());
            }
        });
    }

    private void releaseStock(Order order) {
        for (var item : order.getItems()) {
            if (item.getProduct() != null) {
                item.getProduct().restoreStock(item.getQuantity());
            }
        }
    }

    private void requirePending(Order order, String message) {
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException(message);
        }
    }

    private Order ownedOrder(UUID customerId, UUID orderId) {
        return orders.findById(orderId)
            .filter(order -> order.getCustomer().getId().equals(customerId))
            .orElseThrow(() -> new NotFoundException("Order was not found"));
    }
}
