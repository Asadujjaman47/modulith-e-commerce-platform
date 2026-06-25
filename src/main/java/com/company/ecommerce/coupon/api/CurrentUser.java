package com.company.ecommerce.coupon.api;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Resolves the authenticated user's id from the security context. The JWT filter stores the user id
 * as the authentication name, so this works without depending on auth module internals.
 */
final class CurrentUser {

    private CurrentUser() {}

    static UUID id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(authentication.getName());
    }
}
