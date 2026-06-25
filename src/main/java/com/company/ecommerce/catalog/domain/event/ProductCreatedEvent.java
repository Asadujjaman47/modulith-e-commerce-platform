package com.company.ecommerce.catalog.domain.event;

import java.util.UUID;

/**
 * Published when a new product is created. Consumed by the {@code inventory} module to seed a
 * stock record for the product.
 */
public record ProductCreatedEvent(UUID productId, String sku) {}