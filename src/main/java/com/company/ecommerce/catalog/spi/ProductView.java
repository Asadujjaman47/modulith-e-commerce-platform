package com.company.ecommerce.catalog.spi;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lightweight, read-only projection of a catalog product for cross-module consumers. Carries only
 * the fields other modules legitimately need (no aggregate, no JPA entity leaks across boundaries).
 */
public record ProductView(UUID id, String name, String sku, BigDecimal price, boolean active) {}
