package com.company.ecommerce.catalog.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Brand aggregate root. Owned by the {@code catalog} module. */
@Entity
@Table(name = "brands")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Brand extends AuditableEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "description")
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "active", nullable = false)
    private boolean active;

    private Brand(String name, String slug, String description, String logoUrl) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.logoUrl = logoUrl;
        this.active = true;
    }

    public static Brand create(String name, String slug, String description, String logoUrl) {
        return new Brand(name, slug, description, logoUrl);
    }

    public void update(
            String name, String slug, String description, String logoUrl, boolean active) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.logoUrl = logoUrl;
        this.active = active;
    }
}