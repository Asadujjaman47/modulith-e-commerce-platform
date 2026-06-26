package com.company.ecommerce.payment.application;

import com.company.ecommerce.order.spi.OrderLifecycle;
import com.company.ecommerce.payment.domain.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Applies a successful payment's downstream effect on the order: marking it {@code PAID} through the
 * order {@code spi}. Runs after the payment transaction commits, in its own transaction, so the
 * cross-module write stays within the order module's boundary (payment → order, never the reverse).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFulfillmentHandlers {

    private final OrderLifecycle orderLifecycle;

    @ApplicationModuleListener
    public void markOrderPaid(PaymentCompletedEvent event) {
        orderLifecycle.markPaid(event.orderId());
        log.info("Marked order {} as paid following payment {}.", event.orderId(), event.paymentId());
    }
}
