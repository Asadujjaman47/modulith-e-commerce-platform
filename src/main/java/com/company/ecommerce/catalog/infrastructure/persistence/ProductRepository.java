package com.company.ecommerce.catalog.infrastructure.persistence;

import com.company.ecommerce.catalog.domain.Product;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Persistence for {@link Product} aggregates. Extends {@link JpaSpecificationExecutor} to support
 * dynamic, paginated catalog search and filtering.
 */
public interface ProductRepository
        extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, UUID id);
}