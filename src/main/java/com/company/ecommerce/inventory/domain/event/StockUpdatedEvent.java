package com.company.ecommerce.inventory.domain.event;

import java.util.UUID;

/** Published when a product's on-hand stock level changes. */
public record StockUpdatedEvent(UUID productId, int quantityOnHand, int quantityAvailable) {}