package com.fragrance.ecommerce.orders;

import jakarta.validation.constraints.NotBlank;

public final class OrderDtos {
    private OrderDtos() {}
    public record CreateRequest(@NotBlank String idempotencyKey) {}
    public record OrderResponse(java.util.UUID id, String status, java.math.BigDecimal total,
                                java.util.List<OrderItemResponse> items) {}
    public record OrderItemResponse(String productName, java.math.BigDecimal unitPrice,
                                    int quantity, java.math.BigDecimal lineTotal) {}
}
