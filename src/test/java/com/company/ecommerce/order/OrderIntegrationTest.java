package com.company.ecommerce.order;

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
 * End-to-end coverage of the Phase 4 order flows against real PostgreSQL and Redis: a registered
 * customer adds a stocked product to their cart and places an order; the cart is cleared and stock is
 * reserved asynchronously; the order is viewable in details/history but not by other customers; an
 * admin walks the status lifecycle; cancellation releases the reserved stock; coupons, empty-cart
 * rejection, idempotent retries and admin-only authorization are all exercised.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;

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

    /** Registers and logs in a customer, returning an access token once the profile exists. */
    private String registerCustomer() throws Exception {
        String email = "buyer-" + System.nanoTime() + "@example.com";
        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"%s","password":"%s","firstName":"Buyer","lastName":"One"}
                                        """
                                                .formatted(email, PASSWORD)))
                .andExpect(status().isCreated());

        MvcResult loginResult =
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"email":"%s","password":"%s"}
                                                """
                                                        .formatted(email, PASSWORD)))
                        .andExpect(status().isOk())
                        .andReturn();
        String token = data(loginResult).get("accessToken").asText();

        // Customer profile is created asynchronously via UserRegisteredEvent.
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () ->
                                mockMvc.perform(bearer(get("/api/v1/users/me"), token))
                                        .andExpect(status().isOk()));
        return token;
    }

    private String createAddress(String token) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                bearer(post("/api/v1/users/me/addresses"), token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"label":"Home","line1":"221B Baker St","city":"London","postalCode":"NW1 6XE","country":"GB","defaultAddress":true}
                                                """))
                        .andExpect(status().isCreated())
                        .andReturn();
        return data(result).get("id").asText();
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

    private void addToCart(String token, String productId, int quantity) throws Exception {
        mockMvc.perform(
                        bearer(post("/api/v1/cart/items"), token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"productId":"%s","quantity":%d}
                                        """
                                                .formatted(productId, quantity)))
                .andExpect(status().isCreated());
    }

    @Test
    void fullOrderLifecycle() throws Exception {
        String admin = adminToken();
        String customer = registerCustomer();
        String addressId = createAddress(customer);
        String productId = createStockedProduct(admin, 50);
        addToCart(customer, productId, 3);

        // Place the order from the cart.
        MvcResult placeResult =
                mockMvc.perform(
                                bearer(post("/api/v1/orders"), customer)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"addressId":"%s"}
                                                """
                                                        .formatted(addressId)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.status", is("PENDING")))
                        .andExpect(jsonPath("$.data.items.length()", is(1)))
                        .andExpect(jsonPath("$.data.subtotal", is(300.00)))
                        .andExpect(jsonPath("$.data.totalAmount", is(300.00)))
                        .andExpect(jsonPath("$.data.shippingAddress.city", is("London")))
                        .andReturn();
        String orderId = data(placeResult).get("id").asText();

        // The cart is cleared asynchronously after OrderCreatedEvent.
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () ->
                                mockMvc.perform(bearer(get("/api/v1/cart"), customer))
                                        .andExpect(jsonPath("$.data.items.length()", is(0))));

        // Stock is reserved asynchronously for the order.
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () ->
                                mockMvc.perform(
                                                bearer(
                                                        get("/api/v1/admin/inventory/" + productId),
                                                        admin))
                                        .andExpect(jsonPath("$.data.quantityReserved", is(3)))
                                        .andExpect(jsonPath("$.data.quantityAvailable", is(47))));

        // Order details and history are visible to the owner.
        mockMvc.perform(bearer(get("/api/v1/orders/" + orderId), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PENDING")));
        mockMvc.perform(bearer(get("/api/v1/orders"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.content[0].itemCount", is(1)));

        // Another customer cannot see this order.
        String otherCustomer = registerCustomer();
        mockMvc.perform(bearer(get("/api/v1/orders/" + orderId), otherCustomer))
                .andExpect(status().isNotFound());

        // Cancel, then the reserved stock is released asynchronously.
        mockMvc.perform(bearer(post("/api/v1/orders/" + orderId + "/cancel"), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("CANCELLED")));
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () ->
                                mockMvc.perform(
                                                bearer(
                                                        get("/api/v1/admin/inventory/" + productId),
                                                        admin))
                                        .andExpect(jsonPath("$.data.quantityReserved", is(0)))
                                        .andExpect(jsonPath("$.data.quantityAvailable", is(50))));
    }

    @Test
    void placesOrderWithCoupon() throws Exception {
        String admin = adminToken();
        String customer = registerCustomer();
        String addressId = createAddress(customer);
        String productId = createStockedProduct(admin, 50);
        addToCart(customer, productId, 2); // subtotal 200.00

        String code = "SAVE10-" + System.nanoTime();
        mockMvc.perform(
                        bearer(post("/api/v1/admin/coupons"), admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"code":"%s","description":"10%% off","discountType":"PERCENTAGE","discountValue":10,"minOrderAmount":50.00,"validFrom":"%s","validUntil":"%s","usageLimit":100}
                                        """
                                                .formatted(
                                                        code,
                                                        Instant.now()
                                                                .minus(Duration.ofDays(1))
                                                                .toString(),
                                                        Instant.now()
                                                                .plus(Duration.ofDays(30))
                                                                .toString())))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        bearer(post("/api/v1/orders"), customer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"addressId":"%s","couponCode":"%s"}
                                        """
                                                .formatted(addressId, code)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.couponCode", is(code)))
                .andExpect(jsonPath("$.data.subtotal", is(200.00)))
                .andExpect(jsonPath("$.data.discountAmount", is(20.00)))
                .andExpect(jsonPath("$.data.totalAmount", is(180.00)));
    }

    @Test
    void rejectsPlacingOrderWithEmptyCart() throws Exception {
        String customer = registerCustomer();
        String addressId = createAddress(customer);

        mockMvc.perform(
                        bearer(post("/api/v1/orders"), customer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"addressId":"%s"}
                                        """.formatted(addressId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void adminWalksStatusLifecycle() throws Exception {
        String admin = adminToken();
        String customer = registerCustomer();
        String addressId = createAddress(customer);
        String productId = createStockedProduct(admin, 50);
        addToCart(customer, productId, 1);

        MvcResult placeResult =
                mockMvc.perform(
                                bearer(post("/api/v1/orders"), customer)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                {"addressId":"%s"}
                                                """.formatted(addressId)))
                        .andExpect(status().isCreated())
                        .andReturn();
        String orderId = data(placeResult).get("id").asText();

        for (String next : new String[] {"PROCESSING", "SHIPPED", "DELIVERED"}) {
            mockMvc.perform(
                            bearer(put("/api/v1/admin/orders/" + orderId + "/status"), admin)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {"status":"%s"}
                                            """.formatted(next)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status", is(next)));
        }

        // An illegal transition (DELIVERED -> PENDING) is rejected.
        mockMvc.perform(
                        bearer(put("/api/v1/admin/orders/" + orderId + "/status"), admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"status":"PENDING"}
                                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void nonAdminCannotManageOrders() throws Exception {
        String customer = registerCustomer();

        mockMvc.perform(bearer(get("/api/v1/admin/orders"), customer))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        bearer(
                                        put(
                                                "/api/v1/admin/orders/"
                                                        + UUID.randomUUID()
                                                        + "/status"),
                                        customer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"status":"PROCESSING"}
                                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void placeOrderIsIdempotent() throws Exception {
        String admin = adminToken();
        String customer = registerCustomer();
        String addressId = createAddress(customer);
        String productId = createStockedProduct(admin, 50);
        addToCart(customer, productId, 1);
        String key = UUID.randomUUID().toString();

        MvcResult first =
                mockMvc.perform(
                                bearer(post("/api/v1/orders"), customer)
                                        .header("Idempotency-Key", key)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                {"addressId":"%s"}
                                                """.formatted(addressId)))
                        .andExpect(status().isCreated())
                        .andReturn();
        String firstId = data(first).get("id").asText();

        // Replaying with the same key returns the original order (cart is already cleared).
        MvcResult second =
                mockMvc.perform(
                                bearer(post("/api/v1/orders"), customer)
                                        .header("Idempotency-Key", key)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                {"addressId":"%s"}
                                                """.formatted(addressId)))
                        .andExpect(status().isCreated())
                        .andReturn();
        String secondId = data(second).get("id").asText();

        org.assertj.core.api.Assertions.assertThat(secondId).isEqualTo(firstId);
    }
}
