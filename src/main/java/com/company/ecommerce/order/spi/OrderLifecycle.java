package com.company.ecommerce.order.spi;

import java.util.UUID;

/**
 * Command API for advancing an order through its status lifecycle on behalf of the {@code payment}
 * and {@code shipment} modules. Implemented inside the {@code order} module.
 *
 * <p>Each method has <strong>"ensure at least"</strong>, idempotent semantics: it walks the order
 * forward along the canonical happy path ({@code PENDING → PAID → PROCESSING → SHIPPED → DELIVERED})
 * up to the requested status, and is a no-op when the order is already at (or past) it, or has left the
 * happy path (e.g. {@code CANCELLED}). This keeps the cross-module coupling tolerant of event ordering
 * and retries: a caller expresses intent ("this order has been paid") without needing to know — or
 * race on — the exact current status. For example {@code markDelivered} on a {@code PROCESSING} order
 * transparently passes through {@code SHIPPED}.
 */
public interface OrderLifecycle {

    /** Ensures the order is at least {@code PAID}. */
    void markPaid(UUID orderId);

    /** Ensures the order is at least {@code PROCESSING}. */
    void markProcessing(UUID orderId);

    /** Ensures the order is {@code DELIVERED}, passing through {@code SHIPPED} as needed. */
    void markDelivered(UUID orderId);
}
