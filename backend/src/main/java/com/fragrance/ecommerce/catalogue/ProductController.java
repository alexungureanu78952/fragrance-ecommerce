package com.fragrance.ecommerce.catalogue;

import com.fragrance.ecommerce.domain.Product;
import com.fragrance.ecommerce.service.AuditLogService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.fragrance.ecommerce.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1")
public class ProductController {
    private final ProductService productService;
    private final AuditLogService auditLogService;

    public ProductController(ProductService productService, AuditLogService auditLogService) {
        this.productService = productService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/products")
    public ProductPage list(@PageableDefault(size = 20) Pageable pageable) {
        return ProductPage.from(productService.list(pageable));
    }

    @GetMapping("/products/{id}")
    public ProductDtos.ProductResponse get(@PathVariable UUID id) {
        return toResponse(productService.getPublic(id));
    }

    @PostMapping("/admin/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDtos.ProductResponse create(@Valid @RequestBody ProductDtos.ProductRequest request,
                                               @AuthenticationPrincipal AuthenticatedUser user) {
        var product = productService.create(request);
        auditLogService.record(user.id(), "CREATE", "PRODUCT", product.getId().toString(),
            "SUCCESS", null);
        return toResponse(product);
    }

    @PatchMapping("/admin/products/{id}")
    public ProductDtos.ProductResponse update(@PathVariable UUID id,
                                               @Valid @RequestBody ProductDtos.ProductRequest request,
                                               @AuthenticationPrincipal AuthenticatedUser user) {
        var product = productService.update(id, request);
        auditLogService.record(user.id(), "UPDATE", "PRODUCT", id.toString(), "SUCCESS", null);
        return toResponse(product);
    }

    @DeleteMapping("/admin/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        productService.deactivate(id);
        auditLogService.record(user.id(), "DEACTIVATE", "PRODUCT", id.toString(), "SUCCESS", null);
    }

    private ProductDtos.ProductResponse toResponse(Product product) {
        return new ProductDtos.ProductResponse(product.getId(), product.getBrand().getId(),
            product.getBrand().getName(), product.getCategory().getId(), product.getCategory().getName(),
            product.getName(), product.getDescription(), product.getPrice(), product.getStockQuantity());
    }

    public record ProductPage(java.util.List<ProductDtos.ProductResponse> items, int page, int size,
                              long totalItems, int totalPages) {
        static ProductPage from(Page<Product> page) {
            var items = page.getContent().stream().map(product -> new ProductDtos.ProductResponse(
                product.getId(), product.getBrand().getId(), product.getBrand().getName(),
                product.getCategory().getId(), product.getCategory().getName(), product.getName(),
                product.getDescription(), product.getPrice(), product.getStockQuantity())).toList();
            return new ProductPage(items, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
        }
    }
}
