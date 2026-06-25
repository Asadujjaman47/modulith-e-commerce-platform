package com.company.ecommerce.catalog.domain.event;

import java.util.UUID;

/** Published when a product is deleted. */
public record ProductDeletedEvent(UUID productId) {}