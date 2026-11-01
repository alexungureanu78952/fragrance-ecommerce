package com.fragrance.ecommerce.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.fragrance.ecommerce.domain.Brand;
import com.fragrance.ecommerce.domain.Cart;
import com.fragrance.ecommerce.domain.Category;
import com.fragrance.ecommerce.domain.Customer;
import com.fragrance.ecommerce.domain.Product;
import com.fragrance.ecommerce.domain.Role;
import com.fragrance.ecommerce.repository.CartRepository;
import com.fragrance.ecommerce.repository.OrderRepository;
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
class OrderServiceTest {
    @Mock CartRepository carts;
    @Mock OrderRepository orders;
    @Mock ProductRepository products;
    @Mock UserRepository users;
    private OrderService service;
    private Customer customer;
    private Product product;
    private Cart cart;

    @BeforeEach
    void setUp() {
        service = new OrderService(carts, orders, products, users);
        customer = new Customer("customer@example.test", "hash", "Customer",
            new Role(UUID.randomUUID(), "CUSTOMER"));
        product = new Product(new Brand("Brand", "Description"),
            new Category("Category", "Description"), "Scent", "Description",
            BigDecimal.TEN, 3);
        cart = new Cart(customer);
        cart.addItem(product, 2);
        when(orders.findByCustomerIdAndIdempotencyKey(customer.getId(), "key"))
            .thenReturn(Optional.empty());
        when(carts.findByCustomerId(customer.getId())).thenReturn(Optional.of(cart));
        when(products.findById(product.getId())).thenReturn(Optional.of(product));
        lenient().when(users.findById(customer.getId())).thenReturn(Optional.of(customer));
        lenient().when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsImmutableSnapshotsAndDecrementsStock() {
        var order = service.create(customer.getId(), "key");
        assertThat(order.getTotal()).isEqualByComparingTo("20.00");
        assertThat(order.getItems().getFirst().getProductNameSnapshot()).isEqualTo("Scent");
        assertThat(product.getStockQuantity()).isEqualTo(1);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void repeatsWithSameIdempotencyKeyWithoutCreatingAnotherOrder() {
        var existing = service.create(customer.getId(), "key");
        when(orders.findByCustomerIdAndIdempotencyKey(customer.getId(), "key"))
            .thenReturn(Optional.of(existing));
        assertThat(service.create(customer.getId(), "key")).isSameAs(existing);
    }

    @Test
    void rejectsInsufficientStock() {
        product.decreaseStock(2);
        assertThatThrownBy(() -> service.create(customer.getId(), "key"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Insufficient stock");
    }
}
