package com.company.ecommerce.config.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health contributor that reports on the observability pipeline itself: it confirms the
 * Micrometer registry is wired and collecting meters, and surfaces the meter count under
 * {@code /actuator/health} (visible to authorized callers).
 *
 * <p>Registered automatically as the {@code metrics} health contributor by virtue of the bean name
 * suffix convention. It contributes to the overall health status but never reports {@code DOWN} for
 * a benign empty registry, so it does not gate readiness.
 */
@Component("metricsHealthIndicator")
public class MetricsHealthIndicator implements HealthIndicator {

    private final MeterRegistry registry;

    public MetricsHealthIndicator(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Health health() {
        int meterCount = registry.getMeters().size();
        return Health.up()
                .withDetail("registry", registry.getClass().getSimpleName())
                .withDetail("meterCount", meterCount)
                .build();
    }
}
