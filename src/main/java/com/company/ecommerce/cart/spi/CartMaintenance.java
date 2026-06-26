package com.company.ecommerce.cart.spi;

import java.util.UUID;

/**
 * Command API for maintaining a customer's cart on behalf of other modules (e.g. {@code order} after
 * an order is placed). Implemented inside the {@code cart} module.
 */
public interface CartMaintenance {

    /** Empties the customer's cart, if they have one. */
    void clear(UUID customerId);
}
