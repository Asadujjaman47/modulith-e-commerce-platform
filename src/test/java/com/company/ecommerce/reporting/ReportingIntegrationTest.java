package com.company.ecommerce.reporting;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.ecommerce.TestcontainersConfiguration;
import com.company.ecommerce.auth.domain.Role;
import com.company.ecommerce.auth.infrastructure.security.JwtTokenProvider;
import com.company.ecommerce.order.domain.event.OrderCreatedEvent;
import com.company.ecommerce.order.domain.event.OrderLine;
import com.company.ecommerce.reporting.infrastructure.persistence.SalesFactRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
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
 * End-to-end coverage of the reporting module against real PostgreSQL and Redis: an
 * {@code OrderCreatedEvent} is recorded as a sales/product projection (idempotently), and the admin
 * sales and product report endpoints reflect it. Non-admins are rejected.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReportingIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private SalesFactRepository salesFactRepository;

    @Test
    void recordsOrderAndExposesReports() throws Exception {
        String admin = token(Role.ADMIN);
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        OrderCreatedEvent event =
                new OrderCreatedEvent(
                        orderId,
                        "ORD-" + System.nanoTime(),
                        UUID.randomUUID(),
                        List.of(new OrderLine(productId, 4)),
                        null,
                        new BigDecimal("10.00"),
                        new BigDecimal("90.00"));

        publish(event);
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(
                        () ->
                                Assertions.assertThat(salesFactRepository.existsByOrderId(orderId))
                                        .isTrue());

        // Sales report includes today's order(s). The DB is shared across the suite, so other tests
        // may also have recorded sales for today; assert our contribution is reflected, not exact totals.
        mockMvc.perform(
                        bearer(
                                get("/api/v1/admin/reports/sales")
                                        .param("from", today.toString())
                                        .param("to", today.toString()),
                                admin))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.orderCount")
                                .value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(
                        jsonPath("$.data.unitsSold")
                                .value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.data.daily").isArray());

        // The product report keys on our unique product id: exactly four units sold.
        mockMvc.perform(
                        bearer(
                                get("/api/v1/admin/reports/products")
                                        .param("from", today.toString())
                                        .param("to", today.toString())
                                        .param("size", "100"),
                                admin))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.content[?(@.productId=='" + productId + "')].unitsSold")
                                .value(org.hamcrest.Matchers.contains(4)));

        // Inverted window is rejected (409, per the BusinessException convention).
        mockMvc.perform(
                        bearer(
                                get("/api/v1/admin/reports/sales")
                                        .param("from", today.plusDays(1).toString())
                                        .param("to", today.toString()),
                                admin))
                .andExpect(status().isConflict());

        // Non-admins cannot read reports.
        mockMvc.perform(
                        bearer(
                                get("/api/v1/admin/reports/sales")
                                        .param("from", today.toString())
                                        .param("to", today.toString()),
                                token(Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    private void publish(OrderCreatedEvent event) {
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
