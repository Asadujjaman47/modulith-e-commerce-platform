package com.company.ecommerce.catalog.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Filter criteria for catalog browse/search. All fields are optional; null fields are ignored.
 *
 * @param keyword free-text match against product name/description/sku
 * @param categoryId restrict to a category
 * @param brandId restrict to a brand
 * @param minPrice inclusive lower price bound
 * @param maxPrice inclusive upper price bound
 * @param activeOnly when true, only active products are returned
 */
public record ProductSearchCriteria(
        String keyword,
        UUID categoryId,
        UUID brandId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        boolean activeOnly) {}