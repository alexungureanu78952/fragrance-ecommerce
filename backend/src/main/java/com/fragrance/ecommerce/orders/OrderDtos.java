package com.fragrance.ecommerce.orders;

import com.fragrance.ecommerce.domain.OrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderDtos {
    private OrderDtos() {}
    public record CreateRequest(@NotBlank String idempotencyKey) {}
    public record OrderResponse(UUID id, String status, BigDecimal total,
                                List<OrderItemResponse> items) {}
    public record OrderItemResponse(String productName, BigDecimal unitPrice,
                                    int quantity, BigDecimal lineTotal) {}
    public record AdminOrderResponse(UUID id, UUID customerId, String customerEmail,
                                     String customerName, String status, BigDecimal total,
                                     Instant createdAt, List<OrderItemResponse> items) {}
    public record StatusUpdateRequest(@NotNull OrderStatus status) {}
}
