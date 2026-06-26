package com.company.ecommerce.shipment.domain.event;

import java.util.UUID;

/**
 * Published when a shipment is created for a paid order.
 *
 * <p>Consumed in later phases by {@code notification}/{@code reporting}/{@code audit}. The
 * {@code shipment} module itself listens for it to advance the order to {@code PROCESSING} via the
 * order {@code spi}.
 */
public record ShipmentCreatedEvent(
        UUID shipmentId, UUID orderId, UUID customerId, String trackingNumber, String carrier) {}
