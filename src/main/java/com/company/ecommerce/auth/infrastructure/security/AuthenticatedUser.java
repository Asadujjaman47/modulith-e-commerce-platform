package com.company.ecommerce.auth.infrastructure.security;

import com.company.ecommerce.auth.domain.Role;
import java.util.UUID;
import org.springframework.security.core.AuthenticatedPrincipal;

/**
 * Authenticated principal extracted from a validated access token and stored in the Spring Security
 * context.
 *
 * <p>{@link #getName()} returns the user id so it flows transparently to JPA auditing
 * ({@code createdBy}/{@code updatedBy}) and to other modules that read
 * {@code Authentication#getName()} — without those modules depending on auth internals.
 *
 * @param userId the auth user id (also used as the auditing principal)
 * @param email the authenticated email
 * @param role the user's role
 */
public record AuthenticatedUser(UUID userId, String email, Role role)
        implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return userId.toString();
    }
}