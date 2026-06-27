package com.company.ecommerce.config.observability;

import com.company.ecommerce.auth.domain.event.UserRegisteredEvent;
import com.company.ecommerce.order.domain.event.OrderCreatedEvent;
import com.company.ecommerce.payment.domain.event.PaymentCompletedEvent;
import com.company.ecommerce.payment.domain.event.PaymentFailedEvent;
import com.company.ecommerce.review.domain.event.ReviewCreatedEvent;
import com.company.ecommerce.shipment.domain.event.ShipmentDeliveredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Translates published domain events into business metric increments.
 *
 * <p>Listens on the same post-commit, transactional {@code @ApplicationModuleListener} channel used
 * by {@code audit}/{@code reporting}, consuming each module's {@code events} named interface. Keeping
 * the meters here (cross-cutting {@code config}) means the business modules stay free of any metrics
 * dependency, exactly as they stay free of the audit/reporting sinks.
 */
@Component
@RequiredArgsConstructor
public class BusinessMetricsEventHandlers {

    private final BusinessMetrics metrics;

    @ApplicationModuleListener
    public void on(OrderCreatedEvent event) {
        metrics.ordersPlaced().increment();
    }

    @ApplicationModuleListener
    public void on(PaymentCompletedEvent event) {
        metrics.paymentsCompleted().increment();
    }

    @ApplicationModuleListener
    public void on(PaymentFailedEvent event) {
        metrics.paymentsFailed().increment();
    }

    @ApplicationModuleListener
    public void on(ShipmentDeliveredEvent event) {
        metrics.shipmentsDelivered().increment();
    }

    @ApplicationModuleListener
    public void on(ReviewCreatedEvent event) {
        metrics.reviewsCreated().increment();
    }

    @ApplicationModuleListener
    public void on(UserRegisteredEvent event) {
        metrics.usersRegistered().increment();
    }
}
