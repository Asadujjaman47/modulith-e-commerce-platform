package com.company.ecommerce.config.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires application-wide Micrometer metrics conventions.
 *
 * <p>Applies common tags ({@code application}, {@code environment}) to every meter so that all time
 * series can be filtered and grouped consistently in Prometheus/Grafana regardless of which module
 * produced them.
 */
@Configuration
public class MetricsConfig {

    /**
     * Tags every published meter with the application name and active environment. The environment
     * defaults to {@code local} when no Spring profile is active.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags(
            @Value("${spring.application.name:ecommerce}") String application,
            @Value("${spring.profiles.active:local}") String environment) {
        return registry ->
                registry.config().commonTags("application", application, "environment", environment);
    }

    /**
     * Caps the cardinality of the HTTP server URI tag so that unbounded/unmatched paths (404 probes,
     * scanners) cannot explode the Prometheus time-series count.
     */
    @Bean
    public MeterFilter httpUriCardinalityLimit() {
        return MeterFilter.maximumAllowableTags(
                "http.server.requests", "uri", 100, MeterFilter.deny());
    }
}
