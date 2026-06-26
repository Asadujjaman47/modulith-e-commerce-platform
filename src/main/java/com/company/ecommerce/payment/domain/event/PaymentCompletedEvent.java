package com.company.ecommerce.payment.domain.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published when a payment is charged successfully.
 *
 * <p>Consumed by {@code shipment} (create a shipment for the paid order) and, in later phases,
 * {@code notification}/{@code reporting}/{@code audit}. The {@code payment} module itself listens for
 * it to mark the order {@code PAID} via the order {@code spi}.
 */
public record PaymentCompletedEvent(
        UUID paymentId, UUID orderId, UUID customerId, BigDecimal amount, String currency) {}
