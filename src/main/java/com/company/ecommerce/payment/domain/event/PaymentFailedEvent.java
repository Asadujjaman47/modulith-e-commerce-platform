package com.company.ecommerce.payment.domain.event;

import java.util.UUID;

/** Published when a payment charge attempt fails. */
public record PaymentFailedEvent(
        UUID paymentId, UUID orderId, UUID customerId, String reason) {}
