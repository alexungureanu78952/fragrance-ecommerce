package com.fragrance.ecommerce.repository;

import com.fragrance.ecommerce.domain.Order;
import com.fragrance.ecommerce.domain.OrderStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);
    Page<Order> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    java.util.List<Order> findByCustomerIdAndStatus(UUID customerId, OrderStatus status);
    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);
}
