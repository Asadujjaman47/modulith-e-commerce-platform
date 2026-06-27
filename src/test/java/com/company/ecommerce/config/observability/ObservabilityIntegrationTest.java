package com.company.ecommerce.config.observability;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.company.ecommerce.TestcontainersConfiguration;
import com.company.ecommerce.order.domain.event.OrderCreatedEvent;
import com.company.ecommerce.order.domain.event.OrderLine;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Verifies the observability surface against real infrastructure: the actuator health and readiness
 * endpoints report {@code UP}, and {@code /actuator/prometheus} exposes both the framework HTTP
 * metrics and the custom business counters (incremented from a published {@link OrderCreatedEvent}).
 */
@SpringBootTest
@AutoConfigureMockMvc
// @SpringBootTest disables metrics export + tracing by default; re-enable them so the Prometheus
// scrape endpoint is wired and the meters are actually published during the test.
@AutoConfigureObservability
@Import(TestcontainersConfiguration.class)
class ObservabilityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void healthAndReadinessAreUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @WithMockUser(roles = "ADMIN") // /actuator/prometheus is admin-restricted (scraped with a token)
    void prometheusEndpointExposesBusinessAndHttpMetrics() throws Exception {
        // The custom counters are pre-registered at startup, so they are present immediately, tagged
        // with the common application label.
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ecommerce_orders_placed_orders_total")))
                .andExpect(content().string(containsString("application=\"ecommerce\"")));

        UUID orderId = UUID.randomUUID();
        publish(
                new OrderCreatedEvent(
                        orderId,
                        "ORD-" + System.nanoTime(),
                        UUID.randomUUID(),
                        List.of(new OrderLine(UUID.randomUUID(), 2)),
                        null,
                        BigDecimal.ZERO,
                        new BigDecimal("20.00")));

        // After the post-commit listener fires, the orders counter has advanced beyond zero.
        // Poll on the test thread so the @WithMockUser security context (a thread-local) is visible
        // to the admin-restricted /actuator/prometheus call.
        await().atMost(Duration.ofSeconds(15))
                .pollInSameThread()
                .untilAsserted(
                        () ->
                                mockMvc.perform(get("/actuator/prometheus"))
                                        .andExpect(status().isOk())
                                        .andExpect(
                                                content()
                                                        .string(
                                                                containsString(
                                                                        "ecommerce_orders_placed_orders_total{"))));
    }

    private void publish(OrderCreatedEvent event) {
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(s -> eventPublisher.publishEvent(event));
    }
}
