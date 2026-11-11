package com.fragrance.ecommerce.domain;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;
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
    public Instant getCreatedAt() { return createdAt; }
    public void markPaid() { transitionTo(OrderStatus.PAID); }
    public void markPaymentFailed() { transitionTo(OrderStatus.PAYMENT_FAILED); }
    public void markCanceled() { transitionTo(OrderStatus.CANCELED); }
    public void markShipped() { transitionTo(OrderStatus.SHIPPED); }
    public void markDelivered() { transitionTo(OrderStatus.DELIVERED); }

    public void transitionTo(OrderStatus next) {
        if (!canTransitionTo(next)) {
            throw new IllegalStateException("Order status cannot transition from " + status + " to " + next);
        }
        status = next;
        updatedAt = Instant.now();
    }

    public boolean canTransitionTo(OrderStatus next) {
        if (next == null || next == status) return false;
        return switch (status) {
            case PENDING_PAYMENT -> next == OrderStatus.PAID || next == OrderStatus.PAYMENT_FAILED || next == OrderStatus.CANCELED;
            case PAID -> next == OrderStatus.SHIPPED || next == OrderStatus.CANCELED;
            case SHIPPED -> next == OrderStatus.DELIVERED;
            case DELIVERED, PAYMENT_FAILED, CANCELED -> false;
        };
    }
}
