package com.fragrance.ecommerce.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "carts")
public class Cart {
    @Id
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Cart() {
    }

    public Cart(Customer customer) {
        this.id = UUID.randomUUID();
        this.customer = customer;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public List<CartItem> getItems() { return List.copyOf(items); }
    public void addItem(Product product, int quantity) {
        items.stream().filter(item -> item.getProduct().getId().equals(product.getId()))
            .findFirst().ifPresentOrElse(
                item -> item.increase(quantity),
                () -> items.add(new CartItem(this, product, quantity)));
        updatedAt = Instant.now();
    }
    public void setQuantity(UUID productId, int quantity) {
        items.stream().filter(item -> item.getProduct().getId().equals(productId))
            .findFirst().ifPresentOrElse(
                item -> item.setQuantity(quantity),
                () -> { throw new IllegalArgumentException("Product is not in the cart"); });
        updatedAt = Instant.now();
    }
    public void remove(UUID productId) {
        items.removeIf(item -> item.getProduct().getId().equals(productId));
        updatedAt = Instant.now();
    }
    public void clear() {
        items.clear();
        updatedAt = Instant.now();
    }
}
