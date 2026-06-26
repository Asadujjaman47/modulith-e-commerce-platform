/**
 * Shipment module: delivery management. (Phase 5)
 *
 * <p>Creates a shipment when an order is paid (consuming the payment {@code events} named interface),
 * snapshotting the delivery address from the order {@code spi}. Tracks the shipment through a guarded
 * status machine ({@code CREATED → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED}), recording
 * an append-only tracking history, and lets the owning customer track it. Publishes
 * {@code ShipmentCreatedEvent} and {@code ShipmentDeliveredEvent}; in-module
 * {@code @ApplicationModuleListener}s advance the order to {@code PROCESSING}/{@code DELIVERED} via the
 * order {@code spi} after commit, in their own transactions. The order never depends back on shipment,
 * keeping the module graph acyclic. References orders and customers by id/value only (no cross-module
 * FKs).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Shipment")
package com.company.ecommerce.shipment;