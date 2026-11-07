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
    @Mock com.fragrance.ecommerce.payments.PaymentService payments;
    private OrderService service;
    private Customer customer;
    private Product product;
    private Cart cart;

    @BeforeEach
    void setUp() {
        service = new OrderService(carts, orders, products, users, payments);
        customer = new Customer("customer@example.test", "hash", "Customer",
            new Role(UUID.randomUUID(), "CUSTOMER"));
        product = new Product(new Brand("Brand", "Description"),
            new Category("Category", "Description"), "Scent", "Description",
            BigDecimal.TEN, 3);
        cart = new Cart(customer);
        cart.addItem(product, 2);
        lenient().when(orders.findByCustomerIdAndIdempotencyKey(customer.getId(), "key"))
            .thenReturn(Optional.empty());
        lenient().when(carts.findByCustomerId(customer.getId())).thenReturn(Optional.of(cart));
        lenient().when(products.findById(product.getId())).thenReturn(Optional.of(product));
        lenient().when(users.findById(customer.getId())).thenReturn(Optional.of(customer));
        lenient().when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsImmutableSnapshotsAndDecrementsStock() {
        var order = service.create(customer.getId(), "key");
        assertThat(order.getTotal()).isEqualByComparingTo("20.00");
        assertThat(order.getItems().getFirst().getProductNameSnapshot()).isEqualTo("Scent");
        assertThat(product.getStockQuantity()).isEqualTo(1);
        // The cart only changes once payment succeeds.
        assertThat(cart.getItems()).singleElement()
            .satisfies(item -> assertThat(item.getQuantity()).isEqualTo(2));
        org.mockito.Mockito.verify(payments).abandonPendingOrders(customer.getId());
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

    @Test
    void listsOrdersWithOptionalStatus() {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        org.springframework.data.domain.Page<com.fragrance.ecommerce.domain.Order> page =
            new org.springframework.data.domain.PageImpl<>(java.util.List.of());
        when(orders.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(page);
        when(orders.findByStatusOrderByCreatedAtDesc(com.fragrance.ecommerce.domain.OrderStatus.PAID, pageable))
            .thenReturn(page);

        assertThat(service.listAll(null, pageable)).isSameAs(page);
        assertThat(service.listAll(com.fragrance.ecommerce.domain.OrderStatus.PAID, pageable)).isSameAs(page);
    }

    @Test
    void adminUpdatesOrderStatusAndRestoresStockOnCancel() {
        var order = service.create(customer.getId(), "key");
        order.markPaid();
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));

        var shipped = service.updateStatus(order.getId(), com.fragrance.ecommerce.domain.OrderStatus.SHIPPED);
        assertThat(shipped.getStatus()).isEqualTo(com.fragrance.ecommerce.domain.OrderStatus.SHIPPED);

        var delivered = service.updateStatus(order.getId(), com.fragrance.ecommerce.domain.OrderStatus.DELIVERED);
        assertThat(delivered.getStatus()).isEqualTo(com.fragrance.ecommerce.domain.OrderStatus.DELIVERED);
    }

    @Test
    void adminCancelingPendingOrderRestoresStock() {
        var order = service.create(customer.getId(), "key");
        assertThat(product.getStockQuantity()).isEqualTo(1);
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));

        var canceled = service.updateStatus(order.getId(), com.fragrance.ecommerce.domain.OrderStatus.CANCELED);
        assertThat(canceled.getStatus()).isEqualTo(com.fragrance.ecommerce.domain.OrderStatus.CANCELED);
        assertThat(product.getStockQuantity()).isEqualTo(3);
    }

    @Test
    void rejectsDisallowedTransition() {
        var order = service.create(customer.getId(), "key");
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.updateStatus(order.getId(), com.fragrance.ecommerce.domain.OrderStatus.DELIVERED))
            .isInstanceOf(com.fragrance.ecommerce.exception.ConflictException.class)
            .hasMessageContaining("cannot transition from PENDING_PAYMENT to DELIVERED");
        assertThat(product.getStockQuantity()).isEqualTo(1);
    }
}
