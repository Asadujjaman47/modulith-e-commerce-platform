package com.company.ecommerce.payment.domain;

import java.util.Set;

/**
 * Lifecycle status of a {@link Payment}, with the allowed transitions modelled as a small state
 * machine.
 *
 * <p>{@code PENDING} is the freshly-created intent; a charge attempt moves it to {@code SUCCESS} or
 * {@code FAILED}; a {@code FAILED} payment may be retried (back to {@code PENDING}); a successful
 * payment may later be {@code REFUNDED}.
 */
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED;

    /** Returns the statuses this status may legally transition to. */
    public Set<PaymentStatus> allowedTransitions() {
        return switch (this) {
            case PENDING -> Set.of(SUCCESS, FAILED);
            case FAILED -> Set.of(PENDING, SUCCESS);
            case SUCCESS -> Set.of(REFUNDED);
            case REFUNDED -> Set.of();
        };
    }

    /** Whether a direct transition from this status to {@code target} is permitted. */
    public boolean canTransitionTo(PaymentStatus target) {
        return allowedTransitions().contains(target);
    }
}
