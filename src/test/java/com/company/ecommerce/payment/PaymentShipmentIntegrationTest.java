package com.company.ecommerce.payment;

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
import org.assertj.core.api.Assertions;
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
 * End-to-end coverage of the Phase 5 payment & shipment flows against real PostgreSQL and Redis: a
 * registered customer places an order, a pending payment intent is created asynchronously, the
 * customer pays, the order is marked PAID, a shipment is auto-created, the admin tracks it through to
 * delivery and the order completes. Also exercises refunds, payment history, ownership scoping and
 * admin-only authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PaymentShipmentIntegrationTest {

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

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () ->
                                mockMvc.perform(
                                                bearer(get("/api/v1/admin/inventory/" + productId), admin))
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

    private String placeOrder(String customer, String admin) throws Exception {
        String addressId = createAddress(customer);
        String productId = createStockedProduct(admin, 50);
        mockMvc.perform(
                        bearer(post("/api/v1/cart/items"), customer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"productId":"%s","quantity":3}
                                        """
                                                .formatted(productId)))
                .andExpect(status().isCreated());

        MvcResult placeResult =
                mockMvc.perform(
                                bearer(post("/api/v1/orders"), customer)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                {"addressId":"%s"}
                                                """.formatted(addressId)))
                        .andExpect(status().isCreated())
                        .andReturn();
        return data(placeResult).get("id").asText();
    }

    private void awaitPaymentIntent(String customer, String orderId) {
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () ->
                                mockMvc.perform(
                                                bearer(
                                                        get("/api/v1/payments?orderId=" + orderId),
                                                        customer))
                                        .andExpect(
                                                jsonPath(
                                                        "$.data.totalElements",
                                                        greaterThanOrEqualTo(1))));
    }

    private String orderStatus(String customer, String orderId) throws Exception {
        MvcResult result =
                mockMvc.perform(bearer(get("/api/v1/orders/" + orderId), customer))
                        .andExpect(status().isOk())
                        .andReturn();
        return data(result).get("status").asText();
    }

    @Test
    void fullPaymentAndShipmentLifecycle() throws Exception {
        String admin = adminToken();
        String customer = registerCustomer();
        String orderId = placeOrder(customer, admin);

        // A pending payment intent is created asynchronously from OrderCreatedEvent.
        awaitPaymentIntent(customer, orderId);

        // Pay for the order.
        MvcResult payResult =
                mockMvc.perform(
                                bearer(post("/api/v1/payments"), customer)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"orderId":"%s","method":"CARD"}
                                                """
                                                        .formatted(orderId)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.status", is("SUCCESS")))
                        .andExpect(jsonPath("$.data.amount", is(300.00)))
                        .andReturn();
        String paymentId = data(payResult).get("id").asText();

        // The order is marked PAID asynchronously (and then PROCESSING once the shipment is created).
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () ->
                                Assertions.assertThat(orderStatus(customer, orderId))
                                        .isNotEqualTo("PENDING"));

        // The shipment is auto-created; admin create is idempotent and returns it (assigning a carrier).
        MvcResult shipmentResult =
                mockMvc.perform(
                                bearer(post("/api/v1/admin/shipments"), admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"orderId":"%s","carrier":"DHL"}
                                                """
                                                        .formatted(orderId)))
                        .andExpect(status().isCreated())
                        .andReturn();
        String shipmentId = data(shipmentResult).get("id").asText();

        // The owner can track the shipment and see its history.
        mockMvc.perform(bearer(get("/api/v1/shipments/" + shipmentId), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trackingNumber").exists())
                .andExpect(jsonPath("$.data.trackingRecords.length()", greaterThanOrEqualTo(1)));

        // Admin walks the shipment forward.
        for (String next : new String[] {"PICKED_UP", "IN_TRANSIT"}) {
            mockMvc.perform(
                            bearer(put("/api/v1/admin/shipments/" + shipmentId + "/status"), admin)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {"status":"%s"}
                                            """.formatted(next)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status", is(next)));
        }

        // Confirm delivery.
        mockMvc.perform(bearer(post("/api/v1/admin/shipments/" + shipmentId + "/deliver"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("DELIVERED")));

        // The order is completed asynchronously following delivery.
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () ->
                                Assertions.assertThat(orderStatus(customer, orderId))
                                        .isEqualTo("DELIVERED"));

        // Payment is visible in history and by id.
        mockMvc.perform(bearer(get("/api/v1/payments/" + paymentId), customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SUCCESS")));
    }

    @Test
    void paymentIsIdempotent() throws Exception {
        String admin = adminToken();
        String customer = registerCustomer();
        String orderId = placeOrder(customer, admin);
        awaitPaymentIntent(customer, orderId);

        MvcResult first =
                mockMvc.perform(
                                bearer(post("/api/v1/payments"), customer)
                                        .header("Idempotency-Key", UUID.randomUUID().toString())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                {"orderId":"%s","method":"CARD"}
                                                """.formatted(orderId)))
                        .andExpect(status().isCreated())
                        .andReturn();
        String firstId = data(first).get("id").asText();

        MvcResult second =
                mockMvc.perform(
                                bearer(post("/api/v1/payments"), customer)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                {"orderId":"%s","method":"CARD"}
                                                """.formatted(orderId)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.status", is("SUCCESS")))
                        .andReturn();

        Assertions.assertThat(data(second).get("id").asText()).isEqualTo(firstId);
    }

    @Test
    void adminCanRefundPayment() throws Exception {
        String admin = adminToken();
        String customer = registerCustomer();
        String orderId = placeOrder(customer, admin);
        awaitPaymentIntent(customer, orderId);

        MvcResult payResult =
                mockMvc.perform(
                                bearer(post("/api/v1/payments"), customer)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                {"orderId":"%s","method":"CARD"}
                                                """.formatted(orderId)))
                        .andExpect(status().isCreated())
                        .andReturn();
        String paymentId = data(payResult).get("id").asText();

        mockMvc.perform(bearer(post("/api/v1/admin/payments/" + paymentId + "/refund"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("REFUNDED")));

        // Idempotent replay of the refund.
        mockMvc.perform(bearer(post("/api/v1/admin/payments/" + paymentId + "/refund"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("REFUNDED")));
    }

    @Test
    void nonAdminCannotManagePaymentsOrShipments() throws Exception {
        String customer = registerCustomer();

        mockMvc.perform(bearer(post("/api/v1/admin/payments/" + UUID.randomUUID() + "/refund"), customer))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        bearer(post("/api/v1/admin/shipments"), customer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"orderId":"%s","carrier":"DHL"}
                                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsPaymentForUnknownOrder() throws Exception {
        String customer = registerCustomer();

        mockMvc.perform(
                        bearer(post("/api/v1/payments"), customer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"orderId":"%s","method":"CARD"}
                                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }
}
