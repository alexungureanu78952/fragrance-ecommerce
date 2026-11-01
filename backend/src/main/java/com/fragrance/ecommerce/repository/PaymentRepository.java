package com.fragrance.ecommerce.repository;

import com.fragrance.ecommerce.domain.Payment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(UUID orderId);
    Optional<Payment> findByProviderPaymentId(String providerPaymentId);
}
