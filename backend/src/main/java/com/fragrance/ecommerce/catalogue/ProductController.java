package com.fragrance.ecommerce.catalogue;

import com.fragrance.ecommerce.domain.Product;
import com.fragrance.ecommerce.service.AuditLogService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.fragrance.ecommerce.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1")
public class ProductController {
    private final ProductService productService;
    private final AuditLogService auditLogService;
    private final ProductImageService productImageService;

    public ProductController(ProductService productService, AuditLogService auditLogService,
                             ProductImageService productImageService) {
        this.productService = productService;
        this.auditLogService = auditLogService;
        this.productImageService = productImageService;
    }

    @GetMapping("/products")
    public ProductPage list(
        @org.springframework.web.bind.annotation.RequestParam(required = false) String search,
        @org.springframework.web.bind.annotation.RequestParam(required = false) UUID brandId,
        @org.springframework.web.bind.annotation.RequestParam(required = false) UUID categoryId,
        @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
        @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size,
        @org.springframework.web.bind.annotation.RequestParam(defaultValue = "name") String sort,
        @org.springframework.web.bind.annotation.RequestParam(defaultValue = "asc") String direction) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100");
        }
        var sortProperty = switch (sort) {
            case "name" -> "name";
            case "price" -> "price";
            case "createdAt" -> "createdAt";
            default -> throw new IllegalArgumentException("Unsupported sort field");
        };
        var sortDirection = switch (direction.toLowerCase(java.util.Locale.ROOT)) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new IllegalArgumentException("Unsupported sort direction");
        };
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortProperty));
        return ProductPage.from(productService.list(search, brandId, categoryId, pageable));
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

    @PostMapping("/admin/products/{id}/images")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductImageResponse uploadImage(@PathVariable UUID id,
                                            @RequestParam("file") MultipartFile file,
                                            @RequestParam(required = false) String altText,
                                            @AuthenticationPrincipal AuthenticatedUser user) {
        var image = productImageService.store(id, file, altText);
        auditLogService.record(user.id(), "UPLOAD", "PRODUCT_IMAGE", image.getId().toString(),
            "SUCCESS", null);
        return new ProductImageResponse(image.getId(), image.getOriginalFilename(),
            image.getMimeType(), image.getByteSize(), image.getAltText());
    }

    @DeleteMapping("/admin/products/{productId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(@PathVariable UUID productId, @PathVariable UUID imageId,
                            @AuthenticationPrincipal AuthenticatedUser user) {
        productImageService.delete(productId, imageId);
        auditLogService.record(user.id(), "DELETE", "PRODUCT_IMAGE", imageId.toString(),
            "SUCCESS", null);
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

    public record ProductImageResponse(UUID id, String originalFilename, String mimeType,
                                       long byteSize, String altText) {
    }
}
