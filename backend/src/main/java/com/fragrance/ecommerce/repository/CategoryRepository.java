package com.fragrance.ecommerce.repository;

import com.fragrance.ecommerce.domain.Category;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
}
