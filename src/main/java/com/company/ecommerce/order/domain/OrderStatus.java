package com.company.ecommerce.order.domain;

import java.util.Set;

/**
 * Lifecycle status of an {@link Order}.
 *
 * <p>Encapsulates the allowed status transitions as a small state machine. Most forward transitions
 * (to {@code PAID}, {@code SHIPPED}, {@code DELIVERED}, {@code REFUNDED}) are driven by the future
 * payment/shipment modules; the order module itself only places orders ({@code PENDING}) and cancels
 * them ({@code CANCELLED}), plus an admin lifecycle endpoint that walks the remaining states.
 */
public enum OrderStatus {
    PENDING,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED;

    /** Statuses from which a customer may still cancel the order. */
    private static final Set<OrderStatus> CANCELLABLE = Set.of(PENDING, PAID, PROCESSING);

    /** Returns the statuses this status may legally transition to. */
    public Set<OrderStatus> allowedTransitions() {
        return switch (this) {
            case PENDING -> Set.of(PAID, PROCESSING, CANCELLED);
            case PAID -> Set.of(PROCESSING, CANCELLED, REFUNDED);
            case PROCESSING -> Set.of(SHIPPED, CANCELLED);
            case SHIPPED -> Set.of(DELIVERED);
            case DELIVERED -> Set.of(REFUNDED);
            case CANCELLED, REFUNDED -> Set.of();
        };
    }

    /** Whether a direct transition from this status to {@code target} is permitted. */
    public boolean canTransitionTo(OrderStatus target) {
        return allowedTransitions().contains(target);
    }

    /** Whether an order in this status may be cancelled by the customer. */
    public boolean isCancellable() {
        return CANCELLABLE.contains(this);
    }
}
