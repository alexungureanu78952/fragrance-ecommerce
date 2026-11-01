package com.fragrance.ecommerce.catalogue;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public final class ProductDtos {
    private ProductDtos() {
    }

    public record ProductRequest(
        @NotNull UUID brandId,
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 180) String name,
        @NotBlank @Size(max = 4000) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @Min(0) int stockQuantity
    ) {
    }

    public record ProductResponse(
        UUID id, UUID brandId, String brandName, UUID categoryId, String categoryName,
        String name, String description, BigDecimal price, int stockQuantity
    ) {
    }
}
