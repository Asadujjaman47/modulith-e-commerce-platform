package com.company.ecommerce.catalog.infrastructure.persistence;

import com.company.ecommerce.catalog.domain.Category;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link Category} aggregates. */
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);
}