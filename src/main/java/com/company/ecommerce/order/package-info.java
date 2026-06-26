/**
 * Order module: the order lifecycle. (Phase 4)
 *
 * <p>Places orders from a snapshot of the customer's cart (read via the cart/user/coupon {@code spi}
 * named interfaces, with an availability pre-check against the inventory {@code spi}), tracks their
 * status through a guarded state machine, and lets customers view/cancel their own orders. The order
 * module orchestrates the modules it depends on: after an order is placed/cancelled it drives stock
 * reservation/release, coupon-usage recording and cart clearing through their {@code spi} commands.
 * To keep these out of the placing transaction, they are triggered by the order events below via
 * in-module {@code @ApplicationModuleListener}s, so each side effect runs after commit in its own
 * transaction within a single module's boundary. References customers, products and coupons by value
 * only (no cross-module FKs).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Order")
package com.company.ecommerce.order;