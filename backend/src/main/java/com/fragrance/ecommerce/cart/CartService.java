package com.fragrance.ecommerce.cart;

import com.fragrance.ecommerce.domain.Cart;
import com.fragrance.ecommerce.domain.Customer;
import com.fragrance.ecommerce.domain.Product;
import com.fragrance.ecommerce.exception.NotFoundException;
import com.fragrance.ecommerce.repository.CartRepository;
import com.fragrance.ecommerce.repository.ProductRepository;
import com.fragrance.ecommerce.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {
    private final CartRepository carts;
    private final ProductRepository products;
    private final UserRepository users;

    public CartService(CartRepository carts, ProductRepository products, UserRepository users) {
        this.carts = carts;
        this.products = products;
        this.users = users;
    }
    @Transactional
    public Cart getOrCreate(UUID customerId) {
        return carts.findByCustomerId(customerId).orElseGet(() -> {
            var user = users.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer was not found"));
            if (!(user instanceof Customer customer)) {
                throw new NotFoundException("Customer was not found");
            }
            return carts.save(new Cart(customer));
        });
    }
    @Transactional
    public Cart add(UUID customerId, UUID productId, int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("Quantity must be positive");
        var product = product(productId);
        var cart = getOrCreate(customerId);
        var existingQuantity = cart.getItems().stream()
            .filter(item -> item.getProduct().getId().equals(productId))
            .mapToInt(com.fragrance.ecommerce.domain.CartItem::getQuantity)
            .findFirst().orElse(0);
        ensureStock(product, existingQuantity + quantity);
        cart.addItem(product, quantity);
        return cart;
    }
    @Transactional
    public Cart setQuantity(UUID customerId, UUID productId, int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("Quantity must be positive");
        var product = product(productId);
        ensureStock(product, quantity);
        var cart = getOrCreate(customerId);
        cart.setQuantity(productId, quantity);
        return cart;
    }
    @Transactional
    public Cart remove(UUID customerId, UUID productId) {
        var cart = getOrCreate(customerId);
        cart.remove(productId);
        return cart;
    }
    private Product product(UUID id) {
        return products.findById(id).filter(Product::isActive)
            .orElseThrow(() -> new NotFoundException("Product was not found"));
    }
    private void ensureStock(Product product, int quantity) {
        if (product.getStockQuantity() < quantity) {
            throw new IllegalArgumentException("Insufficient stock");
        }
    }
}
