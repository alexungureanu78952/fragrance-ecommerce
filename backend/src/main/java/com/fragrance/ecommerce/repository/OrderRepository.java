package com.fragrance.ecommerce.repository;

import com.fragrance.ecommerce.domain.Order;
import com.fragrance.ecommerce.domain.OrderStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    @EntityGraph(attributePaths = {"items", "customer"})
    Optional<Order> findById(UUID id);

    @EntityGraph(attributePaths = {"items", "customer"})
    Optional<Order> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);

    @EntityGraph(attributePaths = {"items", "customer"})
    Page<Order> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "customer"})
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"items", "customer"})
    java.util.List<Order> findByCustomerIdAndStatus(UUID customerId, OrderStatus status);

    @EntityGraph(attributePaths = {"items", "customer"})
    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);
}
