package com.company.ecommerce.user.domain.event;

import java.util.UUID;

/**
 * Published when a customer profile is updated.
 *
 * @param customerId the customer aggregate id
 * @param userId the linked auth user id
 */
public record CustomerUpdatedEvent(UUID customerId, UUID userId) {}