package com.company.ecommerce.catalog.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Product category aggregate root. Owned by the {@code catalog} module.
 *
 * <p>Supports an optional self-reference ({@code parentCategoryId}) for sub-categories. The parent
 * is referenced by value only — there is no JPA association — to keep the aggregate small.
 */
@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends AuditableEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "description")
    private String description;

    @Column(name = "parent_category_id")
    private UUID parentCategoryId;

    @Column(name = "active", nullable = false)
    private boolean active;

    private Category(String name, String slug, String description, UUID parentCategoryId) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.parentCategoryId = parentCategoryId;
        this.active = true;
    }

    public static Category create(
            String name, String slug, String description, UUID parentCategoryId) {
        return new Category(name, slug, description, parentCategoryId);
    }

    public void update(
            String name, String slug, String description, UUID parentCategoryId, boolean active) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.parentCategoryId = parentCategoryId;
        this.active = active;
    }
}