package com.company.ecommerce.reporting.application;

import java.util.UUID;

/** Aggregated unit sales for a single product (JPQL projection). */
public record ProductSalesRow(UUID productId, long unitsSold, long orderCount) {}
