/**
 * Payment module: payment processing for orders. (Phase 5)
 *
 * <p>Creates a pending payment intent when an order is placed (consuming the order {@code events}
 * named interface), then processes the charge through a pluggable {@code PaymentGateway} when the
 * customer pays. Tracks payment status and history, supports admin refunds, and records an append-only
 * ledger of gateway interactions. On a successful charge it publishes {@code PaymentCompletedEvent};
 * an in-module {@code @ApplicationModuleListener} then advances the order to {@code PAID} through the
 * order {@code spi}, so the cross-module write happens after commit, in its own transaction. The order
 * never depends back on payment, keeping the module graph acyclic. References orders and customers by
 * id/value only (no cross-module FKs).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Payment")
package com.company.ecommerce.payment;