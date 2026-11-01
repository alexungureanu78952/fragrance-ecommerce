package com.fragrance.ecommerce.catalogue;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fragrance.ecommerce.domain.Brand;
import com.fragrance.ecommerce.domain.Category;
import com.fragrance.ecommerce.domain.Product;
import com.fragrance.ecommerce.exception.NotFoundException;
import com.fragrance.ecommerce.repository.ProductImageRepository;
import com.fragrance.ecommerce.repository.ProductRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    @Mock
    private ProductRepository products;

    @Mock
    private ProductImageRepository images;

    @TempDir
    Path tempDir;

    private Product product;
    private ProductImageService service;

    @BeforeEach
    void setUp() {
        product = new Product(new Brand("Brand", "Description"),
            new Category("Category", "Description"), "Product", "Description",
            java.math.BigDecimal.TEN, 1);
        service = new ProductImageService(products, images, tempDir.toString());
    }

    @Test
    void storesDecodedPngWithGeneratedStorageName() throws Exception {
        when(products.findById(product.getId())).thenReturn(Optional.of(product));
        when(images.findByProductIdOrderBySortOrderAsc(product.getId())).thenReturn(java.util.List.of());
        when(images.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var file = new MockMultipartFile("file", "safe.png", "image/png", pngBytes());

        service.store(product.getId(), file, "Product image");

        try (var files = Files.list(tempDir)) {
            var storedFiles = files.toList();
            org.assertj.core.api.Assertions.assertThat(storedFiles).hasSize(1);
            org.assertj.core.api.Assertions.assertThat(storedFiles.getFirst().getFileName().toString())
                .doesNotContain("safe");
        }
    }

    @Test
    void rejectsContentThatDoesNotDecodeAsAnImage() {
        when(products.findById(product.getId())).thenReturn(Optional.of(product));
        var file = new MockMultipartFile("file", "fake.png", "image/png", "not-an-image".getBytes());

        assertThatThrownBy(() -> service.store(product.getId(), file, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("valid image");
    }

    @Test
    void rejectsDisallowedExtension() {
        when(products.findById(product.getId())).thenReturn(Optional.of(product));
        var file = new MockMultipartFile("file", "payload.svg", "image/svg+xml", pngBytes());

        assertThatThrownBy(() -> service.store(product.getId(), file, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("JPEG and PNG");
    }

    @Test
    void rejectsFilesOverTwelveMegabytes() {
        when(products.findById(product.getId())).thenReturn(Optional.of(product));
        var file = new MockMultipartFile("file", "large.png", "image/png",
            new byte[12 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> service.store(product.getId(), file, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("12 MB");
    }

    private byte[] pngBytes() {
        try {
            var image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            var output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
