package com.company.ecommerce.payment.domain.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Published when a successful payment is refunded. */
public record PaymentRefundedEvent(
        UUID paymentId, UUID orderId, UUID customerId, BigDecimal amount) {}
