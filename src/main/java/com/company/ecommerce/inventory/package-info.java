/**
 * Inventory module: stock levels and reservations. (Phase 2)
 *
 * <p>Exposes an {@code spi}: a read-only available-quantity query, plus reserve/release commands so
 * {@code order} can hold and free stock for an order (each reservation tagged with the order id as
 * its reference).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Inventory")
package com.company.ecommerce.inventory;