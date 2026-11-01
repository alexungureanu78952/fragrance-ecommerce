package com.fragrance.ecommerce.catalogue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fragrance.ecommerce.domain.Brand;
import com.fragrance.ecommerce.domain.Category;
import com.fragrance.ecommerce.domain.Product;
import com.fragrance.ecommerce.exception.NotFoundException;
import com.fragrance.ecommerce.repository.BrandRepository;
import com.fragrance.ecommerce.repository.CategoryRepository;
import com.fragrance.ecommerce.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository products;

    @Mock
    private BrandRepository brands;

    @Mock
    private CategoryRepository categories;

    private ProductService service;
    private Brand brand;
    private Category category;

    @BeforeEach
    void setUp() {
        service = new ProductService(products, brands, categories);
        brand = new Brand("Example Brand", "A test brand");
        category = new Category("Woody", "Woody fragrances");
    }

    @Test
    void createLoadsRelationsAndTrimsText() {
        var request = request("  Cedar  ", "  A warm scent  ", BigDecimal.valueOf(49.90), 3);
        when(brands.findById(brand.getId())).thenReturn(Optional.of(brand));
        when(categories.findById(category.getId())).thenReturn(Optional.of(category));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(request);

        assertThat(result.getName()).isEqualTo("Cedar");
        assertThat(result.getDescription()).isEqualTo("A warm scent");
        assertThat(result.getBrand()).isSameAs(brand);
        verify(products).save(any(Product.class));
    }

    @Test
    void createRejectsUnknownBrand() {
        var request = request("Cedar", "A warm scent", BigDecimal.TEN, 1);
        when(brands.findById(brand.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Brand was not found");
    }

    @Test
    void publicGetHidesInactiveProducts() {
        var product = new Product(brand, category, "Cedar", "A warm scent", BigDecimal.TEN, 1);
        product.deactivate();
        when(products.findById(product.getId())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.getPublic(product.getId()))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateChangesProductFields() {
        var product = new Product(brand, category, "Cedar", "Old", BigDecimal.TEN, 1);
        var replacementBrand = new Brand("New Brand", "New");
        var replacementCategory = new Category("Floral", "Floral");
        when(products.findById(product.getId())).thenReturn(Optional.of(product));
        when(brands.findById(replacementBrand.getId())).thenReturn(Optional.of(replacementBrand));
        when(categories.findById(replacementCategory.getId())).thenReturn(Optional.of(replacementCategory));

        service.update(product.getId(), request("Rose", "New", BigDecimal.valueOf(20), 8,
            replacementBrand.getId(), replacementCategory.getId()));

        assertThat(product.getName()).isEqualTo("Rose");
        assertThat(product.getStockQuantity()).isEqualTo(8);
        assertThat(product.getBrand()).isSameAs(replacementBrand);
    }

    @Test
    void deactivateMarksProductInactiveWithoutDeletingIt() {
        var product = new Product(brand, category, "Cedar", "A warm scent", BigDecimal.TEN, 1);
        when(products.findById(product.getId())).thenReturn(Optional.of(product));

        service.deactivate(product.getId());

        assertThat(product.isActive()).isFalse();
    }

    private ProductDtos.ProductRequest request(String name, String description,
                                               BigDecimal price, int stock) {
        return request(name, description, price, stock, brand.getId(), category.getId());
    }

    private ProductDtos.ProductRequest request(String name, String description,
                                               BigDecimal price, int stock,
                                               UUID brandId, UUID categoryId) {
        return new ProductDtos.ProductRequest(brandId, categoryId, name, description, price, stock);
    }
}
