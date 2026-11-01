CREATE TABLE product_images (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    storage_path VARCHAR(255) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(80) NOT NULL,
    byte_size BIGINT NOT NULL CHECK (byte_size > 0 AND byte_size <= 12582912),
    content_hash VARCHAR(64) NOT NULL,
    sort_order INTEGER NOT NULL CHECK (sort_order >= 0),
    alt_text VARCHAR(255) NOT NULL
);

CREATE INDEX idx_product_images_product_id ON product_images(product_id);
