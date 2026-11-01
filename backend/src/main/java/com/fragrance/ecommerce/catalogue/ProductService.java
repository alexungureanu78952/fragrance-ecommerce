package com.fragrance.ecommerce.catalogue;

import com.fragrance.ecommerce.domain.Product;
import com.fragrance.ecommerce.exception.NotFoundException;
import com.fragrance.ecommerce.repository.BrandRepository;
import com.fragrance.ecommerce.repository.CategoryRepository;
import com.fragrance.ecommerce.repository.ProductRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository products;
    private final BrandRepository brands;
    private final CategoryRepository categories;

    public ProductService(ProductRepository products, BrandRepository brands,
                          CategoryRepository categories) {
        this.products = products;
        this.brands = brands;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public Page<Product> list(String search, UUID brandId, UUID categoryId, Pageable pageable) {
        return products.searchActive(normalizeSearch(search), brandId, categoryId, pageable);
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return "";
        }
        return search.trim();
    }

    @Transactional(readOnly = true)
    public Product getPublic(UUID id) {
        return products.findById(id)
            .filter(Product::isActive)
            .orElseThrow(() -> new NotFoundException("Product was not found"));
    }

    @Transactional
    public Product create(ProductDtos.ProductRequest request) {
        var brand = brands.findById(request.brandId())
            .orElseThrow(() -> new NotFoundException("Brand was not found"));
        var category = categories.findById(request.categoryId())
            .orElseThrow(() -> new NotFoundException("Category was not found"));
        return products.save(new Product(brand, category, request.name().trim(),
            request.description().trim(), request.price(), request.stockQuantity()));
    }

    @Transactional
    public Product update(UUID id, ProductDtos.ProductRequest request) {
        var product = products.findById(id)
            .orElseThrow(() -> new NotFoundException("Product was not found"));
        var brand = brands.findById(request.brandId())
            .orElseThrow(() -> new NotFoundException("Brand was not found"));
        var category = categories.findById(request.categoryId())
            .orElseThrow(() -> new NotFoundException("Category was not found"));
        product.update(brand, category, request.name().trim(), request.description().trim(),
            request.price(), request.stockQuantity());
        return product;
    }

    @Transactional
    public void deactivate(UUID id) {
        var product = products.findById(id)
            .orElseThrow(() -> new NotFoundException("Product was not found"));
        product.deactivate();
    }
}
