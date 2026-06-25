package com.company.ecommerce.catalog.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Image belonging to a {@link Product}. A child of the Product aggregate — never referenced or
 * persisted independently of its owning product.
 */
@Entity
@Table(name = "product_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage extends AuditableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "alt_text")
    private String altText;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    ProductImage(Product product, String url, String altText, int sortOrder, boolean primary) {
        this.product = product;
        this.url = url;
        this.altText = altText;
        this.sortOrder = sortOrder;
        this.primary = primary;
    }
}