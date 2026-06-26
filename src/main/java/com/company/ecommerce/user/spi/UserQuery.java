package com.company.ecommerce.user.spi;

import java.util.Optional;
import java.util.UUID;

/**
 * Synchronous read API into the user module. Implemented inside the {@code user} module and consumed
 * by modules allowed to depend on user (e.g. {@code order}).
 */
public interface UserQuery {

    /**
     * Returns the given address if it belongs to the customer identified by their {@code userId}
     * (the auth principal), or empty otherwise (no such customer, unknown address, or not theirs).
     */
    Optional<AddressView> findAddress(UUID userId, UUID addressId);
}
