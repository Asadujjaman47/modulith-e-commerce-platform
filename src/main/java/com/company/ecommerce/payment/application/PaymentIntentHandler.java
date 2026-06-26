package com.company.ecommerce.payment.application;

import com.company.ecommerce.order.domain.event.OrderCreatedEvent;
import com.company.ecommerce.order.spi.OrderQuery;
import com.company.ecommerce.order.spi.OrderView;
import com.company.ecommerce.payment.domain.Payment;
import com.company.ecommerce.payment.infrastructure.persistence.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Creates a {@code PENDING} payment intent when an order is placed, so a payment record exists for the
 * customer to settle. Runs after the order's placing transaction commits, in its own transaction.
 * Idempotent and tolerant of a customer who pays before this fires — {@link CreatePaymentUseCase} also
 * gets-or-creates the intent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentIntentHandler {

    private static final String DEFAULT_CURRENCY = "USD";

    private final PaymentRepository paymentRepository;
    private final OrderQuery orderQuery;

    @ApplicationModuleListener
    public void on(OrderCreatedEvent event) {
        if (event.totalAmount() == null || event.totalAmount().signum() <= 0) {
            return; // Nothing to collect for a zero-total order.
        }
        if (paymentRepository.findByOrderId(event.orderId()).isPresent()) {
            return;
        }
        String currency =
                orderQuery
                        .findById(event.orderId())
                        .map(OrderView::currency)
                        .orElse(DEFAULT_CURRENCY);
        paymentRepository.save(
                Payment.createIntent(
                        event.orderId(), event.customerId(), event.totalAmount(), currency));
        log.info("Created pending payment intent for order {}.", event.orderId());
    }
}
