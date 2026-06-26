package com.company.ecommerce.audit;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.ecommerce.TestcontainersConfiguration;
import com.company.ecommerce.auth.domain.Role;
import com.company.ecommerce.auth.infrastructure.security.JwtTokenProvider;
import com.company.ecommerce.order.domain.event.OrderCreatedEvent;
import com.company.ecommerce.order.domain.event.OrderLine;
import com.company.ecommerce.review.domain.event.ReviewCreatedEvent;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * End-to-end coverage of the audit module against real PostgreSQL and Redis: business events from
 * other modules are translated into audit-trail rows, which the admin can search and view as a
 * per-user activity timeline. Non-admins are rejected.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuditIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void recordsEventsAndExposesSearchAndActivity() throws Exception {
        String admin = token(Role.ADMIN);
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        publish(
                new OrderCreatedEvent(
                        orderId,
                        "ORD-" + System.nanoTime(),
                        customerId,
                        List.of(new OrderLine(UUID.randomUUID(), 1)),
                        null,
                        BigDecimal.ZERO,
                        new BigDecimal("19.99")));
        publish(new ReviewCreatedEvent(reviewId, UUID.randomUUID(), customerId, 5));

        // The order audit row appears for this actor.
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(
                        () ->
                                mockMvc.perform(
                                                bearer(
                                                        get("/api/v1/admin/audit-logs")
                                                                .param("category", "ORDER")
                                                                .param("actorId", customerId.toString()),
                                                        admin))
                                        .andExpect(status().isOk())
                                        .andExpect(
                                                jsonPath("$.data.content[0].eventType")
                                                        .value("OrderCreated"))
                                        .andExpect(
                                                jsonPath("$.data.content[0].entityId")
                                                        .value(orderId.toString())));

        // Searching by entity id narrows to the review.
        mockMvc.perform(
                        bearer(
                                get("/api/v1/admin/audit-logs").param("entityId", reviewId.toString()),
                                admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].category").value("REVIEW"));

        // The activity timeline for the actor shows both events.
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(
                        () ->
                                mockMvc.perform(
                                                bearer(
                                                        get(
                                                                "/api/v1/admin/audit-logs/activity/"
                                                                        + customerId),
                                                        admin))
                                        .andExpect(status().isOk())
                                        .andExpect(
                                                jsonPath("$.data.totalElements").value(2)));

        // Non-admins cannot read the audit trail.
        mockMvc.perform(bearer(get("/api/v1/admin/audit-logs"), token(Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    private void publish(Object event) {
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(s -> eventPublisher.publishEvent(event));
    }

    private String token(Role role) {
        return jwtTokenProvider.generateAccessToken(
                UUID.randomUUID(), role.name().toLowerCase() + "@example.com", role);
    }

    private MockHttpServletRequestBuilder bearer(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }
}
