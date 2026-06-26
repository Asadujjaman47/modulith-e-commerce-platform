package com.company.ecommerce.shipment.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a shipment is delivered.
 *
 * <p>Consumed in later phases by {@code notification}/{@code reporting}/{@code audit}. The
 * {@code shipment} module itself listens for it to mark the order {@code DELIVERED} via the order
 * {@code spi}.
 */
public record ShipmentDeliveredEvent(
        UUID shipmentId, UUID orderId, UUID customerId, Instant deliveredAt) {}
