package com.company.ecommerce.review;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.ecommerce.TestcontainersConfiguration;
import com.company.ecommerce.auth.domain.Role;
import com.company.ecommerce.auth.infrastructure.security.JwtTokenProvider;
import com.company.ecommerce.order.domain.event.OrderCompletedEvent;
import com.company.ecommerce.review.infrastructure.persistence.ReviewEligibilityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * End-to-end coverage of the review module against real PostgreSQL and Redis: an admin creates a
 * product, a purchase-eligible customer reviews it (eligibility granted via {@code OrderCompletedEvent}),
 * the rating summary reflects the review, duplicates and ineligible customers are rejected, an admin
 * moderates the review (hide/restore) keeping the rating in sync, and the owner deletes it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReviewIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ReviewEligibilityRepository eligibilityRepository;

    @Test
    void fullReviewLifecycle() throws Exception {
        String admin = adminToken();
        String productId = createProduct(admin);
        Customer customer = registerCustomer();

        makeEligible(customer.userId());

        // Create a review.
        MvcResult created =
                mockMvc.perform(
                                bearer(post("/api/v1/products/" + productId + "/reviews"), customer.token())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"rating":5,"title":"Excellent","comment":"Works great"}
                                                """))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.authorName").value("Buyer One"))
                        .andExpect(jsonPath("$.data.rating").value(5))
                        .andReturn();
        String reviewId = data(created).get("id").asText();

        // A second review for the same product is rejected.
        mockMvc.perform(
                        bearer(post("/api/v1/products/" + productId + "/reviews"), customer.token())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"rating":3,"title":"Again","comment":"Dup"}
                                        """))
                .andExpect(status().isConflict());

        // Listing returns the review; the summary reflects it.
        mockMvc.perform(bearer(get("/api/v1/products/" + productId + "/reviews"), customer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(
                        bearer(get("/api/v1/products/" + productId + "/reviews/summary"), customer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewCount").value(1))
                .andExpect(jsonPath("$.data.averageRating").value(5.00));

        // An ineligible customer cannot review.
        Customer ineligible = registerCustomer();
        mockMvc.perform(
                        bearer(post("/api/v1/products/" + productId + "/reviews"), ineligible.token())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"rating":4,"title":"No","comment":"Not allowed"}
                                        """))
                .andExpect(status().isConflict());

        // Admin moderation: hide removes the review from the rating, restore adds it back.
        mockMvc.perform(
                        bearer(put("/api/v1/admin/reviews/" + reviewId + "/status"), admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"status":"HIDDEN"}
                                        """))
                .andExpect(status().isOk());
        mockMvc.perform(
                        bearer(get("/api/v1/products/" + productId + "/reviews/summary"), customer.token()))
                .andExpect(jsonPath("$.data.reviewCount").value(0));
        mockMvc.perform(
                        bearer(get("/api/v1/products/" + productId + "/reviews"), customer.token()))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(
                        bearer(put("/api/v1/admin/reviews/" + reviewId + "/status"), admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"status":"PUBLISHED"}
                                        """))
                .andExpect(status().isOk());
        mockMvc.perform(
                        bearer(get("/api/v1/products/" + productId + "/reviews/summary"), customer.token()))
                .andExpect(jsonPath("$.data.reviewCount").value(1));

        // Non-admin cannot moderate.
        mockMvc.perform(
                        bearer(put("/api/v1/admin/reviews/" + reviewId + "/status"), customer.token())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"status":"HIDDEN"}
                                        """))
                .andExpect(status().isForbidden());

        // Owner deletes their review; the rating returns to zero.
        mockMvc.perform(bearer(delete("/api/v1/reviews/" + reviewId), customer.token()))
                .andExpect(status().isOk());
        mockMvc.perform(
                        bearer(get("/api/v1/products/" + productId + "/reviews/summary"), customer.token()))
                .andExpect(jsonPath("$.data.reviewCount").value(0));
    }

    private void makeEligible(UUID customerId) {
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        s ->
                                eventPublisher.publishEvent(
                                        new OrderCompletedEvent(UUID.randomUUID(), customerId)));
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(
                        () ->
                                org.assertj.core.api.Assertions.assertThat(
                                                eligibilityRepository.existsByCustomerId(customerId))
                                        .isTrue());
    }

    private String createProduct(String admin) throws Exception {
        String unique = String.valueOf(System.nanoTime());
        MvcResult categoryResult =
                mockMvc.perform(
                                bearer(post("/api/v1/admin/categories"), admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                {"name":"Gadgets %s"}
                                                """.formatted(unique)))
                        .andExpect(status().isCreated())
                        .andReturn();
        String categoryId = data(categoryResult).get("id").asText();

        MvcResult productResult =
                mockMvc.perform(
                                bearer(post("/api/v1/admin/products"), admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"name":"Gizmo %s","sku":"GZ-%s","price":59.99,"currency":"USD","categoryId":"%s"}
                                                """
                                                        .formatted(unique, unique, categoryId)))
                        .andExpect(status().isCreated())
                        .andReturn();
        return data(productResult).get("id").asText();
    }

    private Customer registerCustomer() throws Exception {
        String email = "reviewer-" + System.nanoTime() + "@example.com";
        MvcResult result =
                mockMvc.perform(
                                post("/api/v1/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"email":"%s","password":"%s","firstName":"Buyer","lastName":"One"}
                                                """
                                                        .formatted(email, PASSWORD)))
                        .andExpect(status().isCreated())
                        .andReturn();
        UUID userId = UUID.fromString(data(result).get("userId").asText());
        String token = jwtTokenProvider.generateAccessToken(userId, email, Role.CUSTOMER);

        // The customer profile is created asynchronously via UserRegisteredEvent.
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(
                        () ->
                                mockMvc.perform(bearer(get("/api/v1/users/me"), token))
                                        .andExpect(status().isOk()));
        return new Customer(userId, token);
    }

    private String adminToken() {
        return jwtTokenProvider.generateAccessToken(
                UUID.randomUUID(), "admin@example.com", Role.ADMIN);
    }

    private MockHttpServletRequestBuilder bearer(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private JsonNode data(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private record Customer(UUID userId, String token) {}
}
