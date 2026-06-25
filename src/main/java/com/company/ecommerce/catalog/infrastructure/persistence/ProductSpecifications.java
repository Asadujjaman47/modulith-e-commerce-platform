package com.company.ecommerce.catalog.infrastructure.persistence;

import com.company.ecommerce.catalog.application.ProductSearchCriteria;
import com.company.ecommerce.catalog.domain.Product;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/** Builds dynamic {@link Specification}s for product search/filtering. */
public final class ProductSpecifications {

    private ProductSpecifications() {}

    public static Specification<Product> matching(ProductSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(criteria.keyword())) {
                String like = "%" + criteria.keyword().trim().toLowerCase() + "%";
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("name")), like),
                                cb.like(cb.lower(root.get("description")), like),
                                cb.like(cb.lower(root.get("sku")), like)));
            }
            if (criteria.categoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), criteria.categoryId()));
            }
            if (criteria.brandId() != null) {
                predicates.add(cb.equal(root.get("brandId"), criteria.brandId()));
            }
            if (criteria.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), criteria.minPrice()));
            }
            if (criteria.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), criteria.maxPrice()));
            }
            if (criteria.activeOnly()) {
                predicates.add(cb.isTrue(root.get("active")));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}