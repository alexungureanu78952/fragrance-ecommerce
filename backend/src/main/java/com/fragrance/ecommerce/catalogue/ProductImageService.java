package com.fragrance.ecommerce.catalogue;

import com.fragrance.ecommerce.domain.ProductImage;
import com.fragrance.ecommerce.exception.NotFoundException;
import com.fragrance.ecommerce.exception.StorageException;
import com.fragrance.ecommerce.repository.ProductImageRepository;
import com.fragrance.ecommerce.repository.ProductRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductImageService {
    private static final long MAX_BYTES = 12L * 1024 * 1024;
    private static final Set<String> EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final Set<String> MIME_TYPES = Set.of("image/jpeg", "image/png");

    private final ProductRepository products;
    private final ProductImageRepository images;
    private final Path uploadRoot;

    public ProductImageService(ProductRepository products, ProductImageRepository images,
                               @Value("${app.upload-dir:uploads}") String uploadDir) {
        this.products = products;
        this.images = images;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Transactional
    public ProductImage store(UUID productId, MultipartFile file, String altText) {
        var product = products.findById(productId)
            .orElseThrow(() -> new NotFoundException("Product was not found"));
        validate(file);
        var original = safeOriginalName(file.getOriginalFilename());
        var extension = original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        var storageName = UUID.randomUUID() + "." + extension;
        var target = uploadRoot.resolve(storageName).normalize();
        if (!target.getParent().equals(uploadRoot)) {
            throw new StorageException("Invalid image storage path", null);
        }

        try {
            Files.createDirectories(uploadRoot);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            var image = images.save(new ProductImage(product, storageName, original,
                file.getContentType(), file.getSize(), sha256(file), images.findByProductIdOrderBySortOrderAsc(productId).size(),
                altText == null || altText.isBlank() ? original : altText.trim()));
            return image;
        } catch (IOException | RuntimeException exception) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            if (exception instanceof StorageException storageException) {
                throw storageException;
            }
            throw new StorageException("Image could not be stored", exception);
        }
    }

    @Transactional
    public void delete(UUID productId, UUID imageId) {
        var image = images.findById(imageId)
            .filter(candidate -> candidate.getProduct().getId().equals(productId))
            .orElseThrow(() -> new NotFoundException("Product image was not found"));
        try {
            Files.deleteIfExists(uploadRoot.resolve(image.getStoragePath()).normalize());
            images.delete(image);
        } catch (IOException exception) {
            throw new StorageException("Image could not be deleted", exception);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("An image file is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Image must not exceed 12 MB");
        }
        var original = safeOriginalName(file.getOriginalFilename());
        var dot = original.lastIndexOf('.');
        var extension = dot > 0 ? original.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        if (!EXTENSIONS.contains(extension) || !MIME_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Only JPEG and PNG images are accepted");
        }
        try (InputStream input = file.getInputStream()) {
            if (ImageIO.read(input) == null) {
                throw new IllegalArgumentException("File content is not a valid image");
            }
        } catch (IOException exception) {
            throw new StorageException("Image could not be inspected", exception);
        }
    }

    private String safeOriginalName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()
            || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new IllegalArgumentException("Image filename is invalid");
        }
        return originalFilename;
    }

    private String sha256(MultipartFile file) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(file.getBytes()));
        } catch (NoSuchAlgorithmException | IOException exception) {
            throw new StorageException("Image hash could not be calculated", exception);
        }
    }
}
