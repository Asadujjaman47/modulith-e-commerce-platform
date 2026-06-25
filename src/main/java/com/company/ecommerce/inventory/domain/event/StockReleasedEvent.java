package com.company.ecommerce.inventory.domain.event;

import java.util.UUID;

/** Published when previously reserved stock is released. */
public record StockReleasedEvent(UUID reservationId, UUID productId, int quantity) {}