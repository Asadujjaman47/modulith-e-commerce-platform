package com.company.ecommerce.order.domain.event;

import java.util.List;
import java.util.UUID;

/**
 * Published when an order is cancelled.
 *
 * <p>Consumed by {@code inventory} to release the stock reserved for the order; in later phases also
 * by {@code notification}/{@code audit}.
 */
public record OrderCancelledEvent(UUID orderId, UUID customerId, List<OrderLine> lines) {}
