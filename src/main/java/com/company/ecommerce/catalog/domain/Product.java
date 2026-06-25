package com.company.ecommerce.catalog.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Product aggregate root. Owned by the {@code catalog} module.
 *
 * <p>References its {@code categoryId} and optional {@code brandId} by value (no cross-aggregate JPA
 * association). {@link ProductImage}s are children of this aggregate and managed through it.
 */
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends AuditableEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "sku", nullable = false, unique = true)
    private String sku;

    @Column(name = "description")
    private String description;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "brand_id")
    private UUID brandId;

    @Column(name = "active", nullable = false)
    private boolean active;

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    private Product(
            String name,
            String slug,
            String sku,
            String description,
            BigDecimal price,
            String currency,
            UUID categoryId,
            UUID brandId) {
        this.name = name;
        this.slug = slug;
        this.sku = sku;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.categoryId = categoryId;
        this.brandId = brandId;
        this.active = true;
    }

    public static Product create(
            String name,
            String slug,
            String sku,
            String description,
            BigDecimal price,
            String currency,
            UUID categoryId,
            UUID brandId) {
        return new Product(name, slug, sku, description, price, currency, categoryId, brandId);
    }

    public void update(
            String name,
            String slug,
            String description,
            BigDecimal price,
            String currency,
            UUID categoryId,
            UUID brandId,
            boolean active) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.categoryId = categoryId;
        this.brandId = brandId;
        this.active = active;
    }

    /** Replaces the product's images, preserving declared ordering. */
    public void replaceImages(List<ImageData> newImages) {
        this.images.clear();
        for (int i = 0; i < newImages.size(); i++) {
            ImageData data = newImages.get(i);
            this.images.add(
                    new ProductImage(this, data.url(), data.altText(), i, data.primary()));
        }
    }

    /** Transient value object describing an image to attach to a product. */
    public record ImageData(String url, String altText, boolean primary) {}
}