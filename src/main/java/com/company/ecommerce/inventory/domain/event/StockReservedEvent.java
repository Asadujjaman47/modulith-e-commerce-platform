package com.company.ecommerce.inventory.domain.event;

import java.util.UUID;

/** Published when stock is reserved for a product. */
public record StockReservedEvent(UUID reservationId, UUID productId, int quantity) {}