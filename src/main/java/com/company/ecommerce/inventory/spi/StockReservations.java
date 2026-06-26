package com.company.ecommerce.inventory.spi;

import java.util.UUID;

/**
 * Command API for reserving and releasing stock on behalf of other modules (e.g. {@code order} at
 * checkout). Implemented inside the {@code inventory} module.
 *
 * <p>Reservations are tagged with the caller's {@code reference} (e.g. an order id) so they can be
 * released as a group later.
 */
public interface StockReservations {

    /** Reserves {@code quantity} units of a product, failing if insufficient stock is available. */
    void reserve(UUID productId, int quantity, String reference);

    /** Releases all active reservations previously created for the given reference. */
    void releaseByReference(String reference);
}
