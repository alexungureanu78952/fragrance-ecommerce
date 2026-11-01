package com.fragrance.ecommerce.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fragrance.ecommerce.domain.Brand;
import com.fragrance.ecommerce.domain.Cart;
import com.fragrance.ecommerce.domain.Category;
import com.fragrance.ecommerce.domain.Customer;
import com.fragrance.ecommerce.domain.Product;
import com.fragrance.ecommerce.domain.Role;
import com.fragrance.ecommerce.repository.CartRepository;
import com.fragrance.ecommerce.repository.ProductRepository;
import com.fragrance.ecommerce.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {
    @Mock CartRepository carts;
    @Mock ProductRepository products;
    @Mock UserRepository users;
    private CartService service;
    private Customer customer;
    private Product product;

    @BeforeEach
    void setUp() {
        service = new CartService(carts, products, users);
        customer = new Customer("customer@example.test", "hash", "Customer",
            new Role(UUID.randomUUID(), "CUSTOMER"));
        product = new Product(new Brand("Brand", "Description"),
            new Category("Category", "Description"), "Scent", "Description",
            BigDecimal.TEN, 3);
        when(carts.findByCustomerId(customer.getId())).thenReturn(Optional.empty());
        when(users.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(carts.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(products.findById(product.getId())).thenReturn(Optional.of(product));
    }

    @Test
    void addCreatesCartAndItem() {
        var cart = service.add(customer.getId(), product.getId(), 2);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().getFirst().getQuantity()).isEqualTo(2);
    }

    @Test
    void addRejectsQuantityBeyondStockIncludingExistingQuantity() {
        var cart = service.add(customer.getId(), product.getId(), 2);
        when(carts.findByCustomerId(customer.getId())).thenReturn(Optional.of(cart));
        assertThatThrownBy(() -> service.add(customer.getId(), product.getId(), 2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Insufficient stock");
    }
}
