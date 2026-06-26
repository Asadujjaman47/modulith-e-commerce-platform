package com.company.ecommerce.order.spi;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only snapshot of an order for other modules ({@code payment}, {@code shipment}).
 *
 * <p>Carries everything those modules need by value — the payable amount/currency for payment and the
 * snapshotted shipping address for shipment — so they never touch the order aggregate or reference its
 * entities.
 */
public record OrderView(
        UUID orderId,
        String orderNumber,
        UUID customerId,
        String status,
        String currency,
        BigDecimal totalAmount,
        String addressLabel,
        String addressLine1,
        String addressLine2,
        String addressCity,
        String addressState,
        String addressPostalCode,
        String addressCountry) {}
