package com.company.ecommerce.user.domain.event;

import java.util.UUID;

/**
 * Published when a customer profile is created.
 *
 * @param customerId the customer aggregate id
 * @param userId the linked auth user id
 * @param email the customer email
 */
public record CustomerCreatedEvent(UUID customerId, UUID userId, String email) {}