package com.company.ecommerce.config.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Domain (business) metrics surfaced alongside the technical JVM/HTTP metrics.
 *
 * <p>These counters are incremented from {@link BusinessMetricsEventHandlers} in response to
 * published domain events, so the meters live in the cross-cutting {@code config} module and no
 * business module needs to know about Micrometer. Counters are registered lazily via the registry's
 * cache; repeated lookups return the same instance.
 */
@Component
public class BusinessMetrics {

    private final MeterRegistry registry;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
        // Pre-register the zero-valued series so dashboards/alerts have a meter to read before the
        // first event arrives (Prometheus rate() needs the series to exist).
        ordersPlaced();
        paymentsCompleted();
        paymentsFailed();
        shipmentsDelivered();
        reviewsCreated();
        usersRegistered();
    }

    /** Orders successfully placed. */
    public Counter ordersPlaced() {
        return Counter.builder("ecommerce.orders.placed")
                .description("Total number of orders placed")
                .baseUnit("orders")
                .register(registry);
    }

    /** Payments that completed successfully. */
    public Counter paymentsCompleted() {
        return Counter.builder("ecommerce.payments.completed")
                .description("Total number of successful payments")
                .baseUnit("payments")
                .register(registry);
    }

    /** Payments that failed at the gateway. */
    public Counter paymentsFailed() {
        return Counter.builder("ecommerce.payments.failed")
                .description("Total number of failed payments")
                .baseUnit("payments")
                .register(registry);
    }

    /** Shipments marked delivered. */
    public Counter shipmentsDelivered() {
        return Counter.builder("ecommerce.shipments.delivered")
                .description("Total number of shipments delivered")
                .baseUnit("shipments")
                .register(registry);
    }

    /** Product reviews created by customers. */
    public Counter reviewsCreated() {
        return Counter.builder("ecommerce.reviews.created")
                .description("Total number of product reviews created")
                .baseUnit("reviews")
                .register(registry);
    }

    /** New user registrations. */
    public Counter usersRegistered() {
        return Counter.builder("ecommerce.users.registered")
                .description("Total number of registered users")
                .baseUnit("users")
                .register(registry);
    }
}
