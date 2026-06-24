package com.company.ecommerce.auth.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a user successfully authenticates.
 *
 * @param userId the auth user id
 * @param email the authenticated email
 * @param occurredAt when the login happened
 */
public record UserLoggedInEvent(UUID userId, String email, Instant occurredAt) {}