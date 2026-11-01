package com.fragrance.ecommerce.catalogue;

import com.fragrance.ecommerce.domain.Brand;
import com.fragrance.ecommerce.domain.Category;
import com.fragrance.ecommerce.repository.BrandRepository;
import com.fragrance.ecommerce.repository.CategoryRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ReferenceDataController {
    private final BrandRepository brands;
    private final CategoryRepository categories;

    public ReferenceDataController(BrandRepository brands, CategoryRepository categories) {
        this.brands = brands;
        this.categories = categories;
    }

    @GetMapping("/brands")
    public List<ReferenceResponse> brands() {
        return brands.findAll().stream().map(brand -> new ReferenceResponse(
            brand.getId(), brand.getName(), brand.getDescription())).toList();
    }

    @GetMapping("/categories")
    public List<ReferenceResponse> categories() {
        return categories.findAll().stream().map(category -> new ReferenceResponse(
            category.getId(), category.getName(), category.getDescription())).toList();
    }

    public record ReferenceResponse(java.util.UUID id, String name, String description) {
    }
}
