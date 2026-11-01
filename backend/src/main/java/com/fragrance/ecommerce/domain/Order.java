package com.fragrance.ecommerce.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 120)
    private String idempotencyKey;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() {
    }

    public Order(Customer customer, String idempotencyKey, BigDecimal total) {
        this.id = UUID.randomUUID();
        this.customer = customer;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.currency = "EUR";
        this.subtotal = total;
        this.total = total;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }
    public void addItem(Product product, int quantity) {
        items.add(new OrderItem(this, product, quantity));
    }
    public UUID getId() { return id; }
    public Customer getCustomer() { return customer; }
    public OrderStatus getStatus() { return status; }
    public String getCurrency() { return currency; }
    public BigDecimal getTotal() { return total; }
    public List<OrderItem> getItems() { return List.copyOf(items); }
    public void markPaid() { transition(OrderStatus.PAID); }
    public void markPaymentFailed() { transition(OrderStatus.PAYMENT_FAILED); }
    public void markCanceled() { transition(OrderStatus.CANCELED); }
    private void transition(OrderStatus next) {
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Order status cannot transition from " + status);
        }
        status = next;
        updatedAt = Instant.now();
    }
}
