package com.fragrance.ecommerce.orders;

import com.fragrance.ecommerce.domain.Cart;
import com.fragrance.ecommerce.domain.Customer;
import com.fragrance.ecommerce.domain.Order;
import com.fragrance.ecommerce.domain.OrderStatus;
import com.fragrance.ecommerce.domain.Product;
import com.fragrance.ecommerce.exception.ConflictException;
import com.fragrance.ecommerce.exception.NotFoundException;
import com.fragrance.ecommerce.repository.CartRepository;
import com.fragrance.ecommerce.repository.OrderRepository;
import com.fragrance.ecommerce.repository.ProductRepository;
import com.fragrance.ecommerce.repository.UserRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final CartRepository carts;
    private final OrderRepository orders;
    private final ProductRepository products;
    private final UserRepository users;
    private final com.fragrance.ecommerce.payments.PaymentService payments;

    public OrderService(CartRepository carts, OrderRepository orders,
                        ProductRepository products, UserRepository users,
                        com.fragrance.ecommerce.payments.PaymentService payments) {
        this.carts = carts; this.orders = orders; this.products = products; this.users = users;
        this.payments = payments;
    }
    @Transactional
    public Order create(UUID customerId, String idempotencyKey) {
        var existing = orders.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey);
        if (existing.isPresent()) return existing.get();
        // A customer who left an earlier checkout unpaid (e.g. with the browser's back
        // button) starts over: close it and release its stock before reserving again.
        payments.abandonPendingOrders(customerId);
        var cart = carts.findByCustomerId(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Cart is empty"));
        if (cart.getItems().isEmpty()) throw new IllegalArgumentException("Cart is empty");
        var total = BigDecimal.ZERO;
        Map<UUID, Product> cartProducts = new HashMap<>();
        for (var item : cart.getItems()) {
            var product = products.findById(item.getProduct().getId())
                .orElseThrow(() -> new NotFoundException("Product was not found"));
            cartProducts.put(product.getId(), product);
            if (!product.isActive() || product.getStockQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock");
            }
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        var user = users.findById(customerId)
            .filter(Customer.class::isInstance)
            .map(Customer.class::cast)
            .orElseThrow(() -> new NotFoundException("Customer was not found"));
        var order = new Order(user, idempotencyKey, total);
        for (var item : cart.getItems()) {
            var product = cartProducts.get(item.getProduct().getId());
            product.decreaseStock(item.getQuantity());
            order.addItem(product, item.getQuantity());
        }
        // The cart is left intact; purchased items leave it only once payment succeeds.
        return orders.save(order);
    }
    @Transactional(readOnly = true)
    public Page<Order> history(UUID customerId, Pageable pageable) {
        return orders.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
    }
    @Transactional(readOnly = true)
    public Order getOwned(UUID customerId, UUID orderId) {
        return orders.findById(orderId).filter(order -> order.getCustomer().getId().equals(customerId))
            .orElseThrow(() -> new NotFoundException("Order was not found"));
    }
    @Transactional(readOnly = true)
    public Page<Order> listAll(OrderStatus status, Pageable pageable) {
        if (status != null) {
            return orders.findByStatusOrderByCreatedAtDesc(status, pageable);
        }
        return orders.findAllByOrderByCreatedAtDesc(pageable);
    }
    @Transactional
    public Order updateStatus(UUID orderId, OrderStatus nextStatus) {
        var order = orders.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order was not found"));
        if (!order.canTransitionTo(nextStatus)) {
            throw new ConflictException("Order status cannot transition from " + order.getStatus() + " to " + nextStatus);
        }
        if (nextStatus == OrderStatus.CANCELED) {
            for (var item : order.getItems()) {
                if (item.getProduct() != null) {
                    item.getProduct().restoreStock(item.getQuantity());
                }
            }
        }
        order.transitionTo(nextStatus);
        return orders.save(order);
    }
}
