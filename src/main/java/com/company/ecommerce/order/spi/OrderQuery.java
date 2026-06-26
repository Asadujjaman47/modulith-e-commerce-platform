package com.company.ecommerce.order.spi;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-only lookup of orders for other modules (e.g. {@code payment} to validate the payable amount,
 * {@code shipment} to snapshot the delivery address). Implemented inside the {@code order} module.
 */
public interface OrderQuery {

    /** Returns the order by id, or empty if no such order exists. */
    Optional<OrderView> findById(UUID orderId);
}
