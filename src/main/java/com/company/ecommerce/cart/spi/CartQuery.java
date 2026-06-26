package com.company.ecommerce.cart.spi;

import java.util.Optional;
import java.util.UUID;

/**
 * Synchronous read API into a customer's cart. Implemented inside the {@code cart} module and
 * consumed by modules allowed to depend on cart (e.g. {@code order} at checkout).
 */
public interface CartQuery {

    /** Returns the customer's cart, or empty if they have never created one. */
    Optional<CartView> findCart(UUID customerId);
}
