package com.company.ecommerce.catalog;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.ecommerce.TestcontainersConfiguration;
import com.company.ecommerce.auth.domain.Role;
import com.company.ecommerce.auth.infrastructure.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * End-to-end coverage of the Phase 2 catalog and inventory flows against real PostgreSQL and Redis:
 * admin creates a brand, category and product; inventory is seeded asynchronously via the catalog
 * {@code ProductCreatedEvent}; products are browsed/searched with pagination; stock is set, reserved
 * and released; and non-admin callers are rejected from admin endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CatalogInventoryIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String adminToken() {
        return jwtTokenProvider.generateAccessToken(
                UUID.randomUUID(), "admin@example.com", Role.ADMIN);
    }

    private String customerToken() {
        return jwtTokenProvider.generateAccessToken(
                UUID.randomUUID(), "customer@example.com", Role.CUSTOMER);
    }

    private MockHttpServletRequestBuilder bearer(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private JsonNode data(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    @Test
    void fullCatalogAndInventoryFlow() throws Exception {
        String admin = adminToken();
        String unique = String.valueOf(System.nanoTime());

        // Create a brand
        MvcResult brandResult =
                mockMvc.perform(
                                bearer(post("/api/v1/admin/brands"), admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"name":"Acme %s"}
                                                """
                                                        .formatted(unique)))
                        .andExpect(status().isCreated())
                        .andReturn();
        String brandId = data(brandResult).get("id").asText();

        // Create a category
        MvcResult categoryResult =
                mockMvc.perform(
                                bearer(post("/api/v1/admin/categories"), admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"name":"Laptops %s"}
                                                """
                                                        .formatted(unique)))
                        .andExpect(status().isCreated())
                        .andReturn();
        String categoryId = data(categoryResult).get("id").asText();

        // Create a product
        MvcResult productResult =
                mockMvc.perform(
                                bearer(post("/api/v1/admin/products"), admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"name":"UltraBook %s","sku":"UB-%s","price":1299.00,"currency":"USD","categoryId":"%s","brandId":"%s","images":[{"url":"https://cdn.example.com/1.jpg","altText":"Front","primary":true}]}
                                                """
                                                        .formatted(unique, unique, categoryId, brandId)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.currency", is("USD")))
                        .andExpect(jsonPath("$.data.images.length()", is(1)))
                        .andReturn();
        String productId = data(productResult).get("id").asText();

        // Inventory is seeded asynchronously by the inventory module via ProductCreatedEvent
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () ->
                                mockMvc.perform(
                                                bearer(
                                                        get("/api/v1/admin/inventory/" + productId),
                                                        admin))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.quantityOnHand", is(0))));

        // Public browse: product is listed and retrievable
        mockMvc.perform(bearer(get("/api/v1/products/" + productId), customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(productId)));

        // Pagination + sorting + filtering by category
        mockMvc.perform(
                        bearer(
                                get("/api/v1/products")
                                        .param("categoryId", categoryId)
                                        .param("page", "0")
                                        .param("size", "10")
                                        .param("sort", "price,desc"),
                                customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size", is(10)))
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.content[0].categoryId", is(categoryId)));

        // Keyword search
        mockMvc.perform(
                        bearer(
                                get("/api/v1/products/search").param("keyword", "UltraBook " + unique),
                                customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].id", is(productId)));

        // Set on-hand stock
        mockMvc.perform(
                        bearer(put("/api/v1/admin/inventory/" + productId), admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"quantityOnHand":50,"reason":"Initial receipt"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantityAvailable", is(50)));

        // Reserve stock
        MvcResult reserveResult =
                mockMvc.perform(
                                bearer(post("/api/v1/admin/inventory/reserve"), admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"productId":"%s","quantity":20,"reference":"order-1"}
                                                """
                                                        .formatted(productId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                        .andReturn();
        String reservationId = data(reserveResult).get("reservationId").asText();

        mockMvc.perform(bearer(get("/api/v1/admin/inventory/" + productId), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantityReserved", is(20)))
                .andExpect(jsonPath("$.data.quantityAvailable", is(30)));

        // Release the reservation
        mockMvc.perform(
                        bearer(post("/api/v1/admin/inventory/release"), admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"reservationId":"%s"}
                                        """.formatted(reservationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("RELEASED")));

        mockMvc.perform(bearer(get("/api/v1/admin/inventory/" + productId), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantityReserved", is(0)))
                .andExpect(jsonPath("$.data.quantityAvailable", is(50)));
    }

    @Test
    void overReservingFailsWithConflict() throws Exception {
        String admin = adminToken();
        String unique = String.valueOf(System.nanoTime());

        MvcResult categoryResult =
                mockMvc.perform(
                                bearer(post("/api/v1/admin/categories"), admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                {"name":"Phones %s"}
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
                                                {"name":"Phone %s","sku":"PH-%s","price":499.00,"currency":"USD","categoryId":"%s"}
                                                """
                                                        .formatted(unique, unique, categoryId)))
                        .andExpect(status().isCreated())
                        .andReturn();
        String productId = data(productResult).get("id").asText();

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () ->
                                mockMvc.perform(
                                                bearer(
                                                        get("/api/v1/admin/inventory/" + productId),
                                                        admin))
                                        .andExpect(status().isOk()));

        // Only 0 on hand: reserving must fail with 409
        mockMvc.perform(
                        bearer(post("/api/v1/admin/inventory/reserve"), admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"productId":"%s","quantity":1}
                                        """
                                                .formatted(productId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void nonAdminCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(
                        bearer(post("/api/v1/admin/brands"), customerToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"name":"Nope"}
                                        """))
                .andExpect(status().isForbidden());
    }
}
