package com.company.ecommerce.catalog.infrastructure.persistence;

import com.company.ecommerce.catalog.domain.Brand;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link Brand} aggregates. */
public interface BrandRepository extends JpaRepository<Brand, UUID> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);
}