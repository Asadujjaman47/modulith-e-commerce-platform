package com.company.ecommerce.catalog.domain.event;

import java.util.UUID;

/** Published when an existing product is updated. */
public record ProductUpdatedEvent(UUID productId) {}