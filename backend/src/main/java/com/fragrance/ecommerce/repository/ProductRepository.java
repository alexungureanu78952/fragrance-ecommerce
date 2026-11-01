package com.fragrance.ecommerce.repository;

import com.fragrance.ecommerce.domain.Product;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    @Query("""
        select p from Product p
        join fetch p.brand b
        join fetch p.category c
        where p.active = true
          and (:search is null or lower(p.name) like lower(concat('%', :search, '%'))
               or lower(p.description) like lower(concat('%', :search, '%'))
               or lower(b.name) like lower(concat('%', :search, '%')))
          and (:brandId is null or b.id = :brandId)
          and (:categoryId is null or c.id = :categoryId)
        """)
    Page<Product> searchActive(@Param("search") String search,
                               @Param("brandId") UUID brandId,
                               @Param("categoryId") UUID categoryId,
                               Pageable pageable);
}
