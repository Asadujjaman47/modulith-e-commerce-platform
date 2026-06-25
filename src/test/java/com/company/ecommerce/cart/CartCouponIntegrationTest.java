package com.company.ecommerce.cart;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import java.time.Instant;
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
 * End-to-end coverage of the Phase 3 cart and coupon flows against real PostgreSQL and Redis: an
 * admin creates a product (inventory seeded asynchronously) and sets stock; a customer adds it to
 * their cart with a price snapshot, updates the quantity, is rejected when exceeding stock, and
 * removes it; an admin creates a coupon which the customer then validates and applies; and non-admin
 * callers are rejected from admin coupon endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CartCouponIntegrationTest {

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

    private String createStockedProduct(String admin, int onHand) throws Exception {
        String unique = String.valueOf(System.nanoTime());

        MvcResult categoryResult =
                mockMvc.perform(
                                bearer(post("/api/v1/admin/categories"), admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                {"name":"Laptops %s"}
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
                                                {"name":"UltraBook %s","sku":"UB-%s","price":100.00,"currency":"USD","categoryId":"%s"}
                                                """
                                                        .formatted(unique, unique, categoryId)))
                        .andExpect(status().isCreated())
                        .andReturn();
        String productId = data(productResult).get("id").asText();

        // Inventory is seeded asynchronously via the catalog ProductCreatedEvent.
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () ->
                                mockMvc.perform(
                                                bearer(
                                                        get("/api/v1/admin/inventory/" + productId),
                                                        admin))
                                        .andExpect(status().isOk()));

        mockMvc.perform(
                        bearer(put("/api/v1/admin/inventory/" + productId), admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"quantityOnHand":%d,"reason":"Initial receipt"}
                                        """
                                                .formatted(onHand)))
                .andExpect(status().isOk());
        return productId;
    }

    @Test
    void fullCartFlow() throws Exception {
        String admin = adminToken();
        String customer = customerToken();
        String productId = createStockedProduct(admin, 50);

        // Add 3 units to the cart; the unit price is snapshotted from the catalog.
        MvcResult addResult =
                mockMvc.perform(
                                bearer(post("/api/v1/cart/items"), customer)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"productId":"%s","quantity":3}
                                                """
                                                        .formatted(productId)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.items.length()", is(1)))
                        .andExpect(jsonPath("$.data.items[0].unitPrice", is(100.00)))
                        .andExpect(jsonPath("$.data.subtotal", is(300.00)))
                        .andReturn();
        String itemId = data(addResult).get("items").get(0).get("id").asText();

        // Cart is retrievable for the same customer.
        mockMvc.perform(bearer(get("/api/v1/cart"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].productId", is(productId)));

        // Update the quantity within available stock.
        mockMvc.perform(
                        bearer(put("/api/v1/cart/items/" + itemId), customer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"quantity":5}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subtotal", is(500.00)));

        // Exceeding available stock is rejected with 409.
        mockMvc.perform(
                        bearer(put("/api/v1/cart/items/" + itemId), customer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"quantity":999}
                                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)));

        // Remove the item.
        mockMvc.perform(bearer(delete("/api/v1/cart/items/" + itemId), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()", is(0)));
    }

    @Test
    void addingMoreThanStockIsRejected() throws Exception {
        String admin = adminToken();
        String customer = customerToken();
        String productId = createStockedProduct(admin, 2);

        mockMvc.perform(
                        bearer(post("/api/v1/cart/items"), customer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"productId":"%s","quantity":5}
                                        """
                                                .formatted(productId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void couponValidateAndApplyFlow() throws Exception {
        String admin = adminToken();
        String customer = customerToken();
        String code = "SAVE10-" + System.nanoTime();
        String validFrom = Instant.now().minus(Duration.ofDays(1)).toString();
        String validUntil = Instant.now().plus(Duration.ofDays(30)).toString();

        mockMvc.perform(
                        bearer(post("/api/v1/admin/coupons"), admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"code":"%s","description":"10%% off","discountType":"PERCENTAGE","discountValue":10,"minOrderAmount":50.00,"validFrom":"%s","validUntil":"%s","usageLimit":100}
                                        """
                                                .formatted(code, validFrom, validUntil)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code", is(code)));

        // Validate against a qualifying order amount.
        mockMvc.perform(
                        bearer(post("/api/v1/coupons/validate"), customer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"code":"%s","orderAmount":200.00}
                                        """
                                                .formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid", is(true)))
                .andExpect(jsonPath("$.data.discountAmount", is(20.00)))
                .andExpect(jsonPath("$.data.finalAmount", is(180.00)));

        // Apply records the usage and returns the discounted amount.
        mockMvc.perform(
                        bearer(post("/api/v1/coupons/apply"), customer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"code":"%s","orderAmount":200.00}
                                        """
                                                .formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.discountAmount", is(20.00)))
                .andExpect(jsonPath("$.data.finalAmount", is(180.00)));

        // Below the minimum order amount is rejected.
        mockMvc.perform(
                        bearer(post("/api/v1/coupons/validate"), customer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"code":"%s","orderAmount":10.00}
                                        """
                                                .formatted(code)))
                .andExpect(status().isConflict());
    }

    @Test
    void nonAdminCannotCreateCoupon() throws Exception {
        mockMvc.perform(
                        bearer(post("/api/v1/admin/coupons"), customerToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"code":"NOPE","discountType":"PERCENTAGE","discountValue":10,"validFrom":"%s","validUntil":"%s"}
                                        """
                                                .formatted(
                                                        Instant.now().toString(),
                                                        Instant.now()
                                                                .plus(Duration.ofDays(1))
                                                                .toString())))
                .andExpect(status().isForbidden());
    }
}
