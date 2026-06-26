package com.company.ecommerce.order.domain.event;

import java.util.UUID;

/**
 * Published when an order reaches {@code DELIVERED}.
 *
 * <p>Consumed in later phases by {@code review} (enable product reviews) and {@code reporting}.
 */
public record OrderCompletedEvent(UUID orderId, UUID customerId) {}
