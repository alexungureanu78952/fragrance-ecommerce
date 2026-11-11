package com.fragrance.ecommerce.repository;

import com.fragrance.ecommerce.domain.Cart;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByCustomerId(UUID customerId);
}
