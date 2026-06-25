package com.company.ecommerce.inventory.spi;

import java.util.UUID;

/**
 * Synchronous read API into inventory stock levels. Implemented inside the {@code inventory} module
 * and consumed by modules allowed to depend on inventory (e.g. {@code cart}).
 */
public interface InventoryQuery {

    /**
     * Returns the available quantity (on-hand minus reserved) for a product, or {@code 0} when no
     * inventory record exists for it yet.
     */
    int availableQuantity(UUID productId);
}
