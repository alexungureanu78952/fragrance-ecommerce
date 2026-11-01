package com.fragrance.ecommerce.repository;

import com.fragrance.ecommerce.domain.Brand;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, UUID> {
}
