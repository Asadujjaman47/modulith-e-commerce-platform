package com.company.ecommerce.auth.domain.event;

import java.util.UUID;

/**
 * Published when a new user successfully registers. Carries the profile fields supplied at
 * registration so the {@code user} module can create the corresponding customer.
 *
 * @param userId the auth user id (links auth credentials to the user profile)
 * @param email the registered email
 * @param firstName given name
 * @param lastName family name
 */
public record UserRegisteredEvent(UUID userId, String email, String firstName, String lastName) {}