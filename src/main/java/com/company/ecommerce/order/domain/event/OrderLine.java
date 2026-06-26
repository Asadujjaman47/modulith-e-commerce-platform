package com.company.ecommerce.order.domain.event;

import java.util.UUID;

/**
 * A single product line carried on order events, by value. Lets consumers (e.g. {@code inventory})
 * act per product without depending on the order aggregate.
 */
public record OrderLine(UUID productId, int quantity) {}
