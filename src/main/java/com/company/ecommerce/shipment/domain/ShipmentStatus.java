package com.company.ecommerce.shipment.domain;

import java.util.Set;

/**
 * Lifecycle status of a {@link Shipment}, with the allowed transitions modelled as a small state
 * machine for normal carrier progress.
 *
 * <p>Note: the "confirm delivery" action ({@link Shipment#markDelivered}) may move a dispatched
 * shipment straight to {@code DELIVERED} as an admin override; the step-by-step transitions below
 * govern the {@code UpdateShipmentStatusUseCase} progression.
 */
public enum ShipmentStatus {
    CREATED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED;

    /** Returns the statuses this status may legally transition to via the step-by-step flow. */
    public Set<ShipmentStatus> allowedTransitions() {
        return switch (this) {
            case CREATED -> Set.of(PICKED_UP);
            case PICKED_UP -> Set.of(IN_TRANSIT);
            case IN_TRANSIT -> Set.of(OUT_FOR_DELIVERY, DELIVERED);
            case OUT_FOR_DELIVERY -> Set.of(DELIVERED);
            case DELIVERED -> Set.of();
        };
    }

    /** Whether a direct transition from this status to {@code target} is permitted. */
    public boolean canTransitionTo(ShipmentStatus target) {
        return allowedTransitions().contains(target);
    }

    /** Whether the shipment has reached its terminal delivered state. */
    public boolean isDelivered() {
        return this == DELIVERED;
    }
}
