package com.fragrance.ecommerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "product_images")
public class ProductImage {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(name = "storage_path", nullable = false, unique = true, length = 255)
    private String storagePath;
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;
    @Column(name = "mime_type", nullable = false, length = 80)
    private String mimeType;
    @Column(name = "byte_size", nullable = false)
    private long byteSize;
    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @Column(name = "alt_text", nullable = false, length = 255)
    private String altText;

    protected ProductImage() {
    }

    public ProductImage(Product product, String storagePath, String originalFilename,
                        String mimeType, long byteSize, String contentHash,
                        int sortOrder, String altText) {
        this.id = UUID.randomUUID();
        this.product = product;
        this.storagePath = storagePath;
        this.originalFilename = originalFilename;
        this.mimeType = mimeType;
        this.byteSize = byteSize;
        this.contentHash = contentHash;
        this.sortOrder = sortOrder;
        this.altText = altText;
    }

    public UUID getId() { return id; }
    public Product getProduct() { return product; }
    public String getStoragePath() { return storagePath; }
    public String getOriginalFilename() { return originalFilename; }
    public String getMimeType() { return mimeType; }
    public long getByteSize() { return byteSize; }
    public String getAltText() { return altText; }
}
