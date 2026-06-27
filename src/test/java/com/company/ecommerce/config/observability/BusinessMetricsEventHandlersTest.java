package com.company.ecommerce.config.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.ecommerce.auth.domain.event.UserRegisteredEvent;
import com.company.ecommerce.order.domain.event.OrderCreatedEvent;
import com.company.ecommerce.payment.domain.event.PaymentCompletedEvent;
import com.company.ecommerce.payment.domain.event.PaymentFailedEvent;
import com.company.ecommerce.review.domain.event.ReviewCreatedEvent;
import com.company.ecommerce.shipment.domain.event.ShipmentDeliveredEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies each domain event increments its corresponding business counter. Uses a real
 * {@link SimpleMeterRegistry} so the meter naming/registration is exercised end to end.
 */
class BusinessMetricsEventHandlersTest {

    private MeterRegistry registry;
    private BusinessMetricsEventHandlers handlers;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        handlers = new BusinessMetricsEventHandlers(new BusinessMetrics(registry));
    }

    @Test
    void countersStartAtZero() {
        assertThat(count("ecommerce.orders.placed")).isZero();
        assertThat(count("ecommerce.payments.failed")).isZero();
    }

    @Test
    void incrementsOrdersPlaced() {
        handlers.on(
                new OrderCreatedEvent(
                        UUID.randomUUID(),
                        "ORD-1",
                        UUID.randomUUID(),
                        List.of(),
                        null,
                        BigDecimal.ZERO,
                        new BigDecimal("10.00")));

        assertThat(count("ecommerce.orders.placed")).isEqualTo(1.0);
    }

    @Test
    void incrementsPaymentCounters() {
        handlers.on(
                new PaymentCompletedEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("10.00"),
                        "USD"));
        handlers.on(
                new PaymentFailedEvent(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "declined"));

        assertThat(count("ecommerce.payments.completed")).isEqualTo(1.0);
        assertThat(count("ecommerce.payments.failed")).isEqualTo(1.0);
    }

    @Test
    void incrementsEngagementCounters() {
        handlers.on(
                new ShipmentDeliveredEvent(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now()));
        handlers.on(new ReviewCreatedEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 5));
        handlers.on(new UserRegisteredEvent(UUID.randomUUID(), "a@b.com", "A", "B"));

        assertThat(count("ecommerce.shipments.delivered")).isEqualTo(1.0);
        assertThat(count("ecommerce.reviews.created")).isEqualTo(1.0);
        assertThat(count("ecommerce.users.registered")).isEqualTo(1.0);
    }

    private double count(String name) {
        return registry.get(name).counter().count();
    }
}
