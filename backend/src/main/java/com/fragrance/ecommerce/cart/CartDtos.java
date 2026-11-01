package com.fragrance.ecommerce.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public final class CartDtos {
    private CartDtos() {}
    public record ItemRequest(@NotNull UUID productId, @Min(1) int quantity) {}
    public record QuantityRequest(@Min(1) int quantity) {}
}
