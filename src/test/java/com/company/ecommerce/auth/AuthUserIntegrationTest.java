package com.company.ecommerce.auth;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.ecommerce.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end coverage of the Phase 1 flows against real PostgreSQL and Redis: register, the
 * event-driven customer creation, login, profile and address management, token refresh and logout.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthUserIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static final String PASSWORD = "Password123!";

    @Test
    void fullRegisterLoginProfileAddressRefreshLogoutFlow() throws Exception {
        String email = "alice-" + System.nanoTime() + "@example.com";

        // Register
        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"%s","password":"%s","firstName":"Alice","lastName":"Liddell"}
                                        """
                                                .formatted(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is(email)));

        // Login
        JsonNode tokens = login(email);
        String accessToken = tokens.get("accessToken").asText();
        String refreshToken = tokens.get("refreshToken").asText();

        // The customer profile is created asynchronously via UserRegisteredEvent.
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () ->
                                mockMvc.perform(bearer(get("/api/v1/users/me"), accessToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.data.email", is(email)))
                                        .andExpect(jsonPath("$.data.firstName", is("Alice"))));

        // Update profile
        mockMvc.perform(
                        bearer(put("/api/v1/users/me"), accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"firstName":"Alice","lastName":"Cooper","phone":"+14155552671"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lastName", is("Cooper")))
                .andExpect(jsonPath("$.data.phone", is("+14155552671")));

        // Add address (first one becomes default automatically)
        mockMvc.perform(
                        bearer(post("/api/v1/users/me/addresses"), accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"label":"Home","line1":"221B Baker St","city":"London","postalCode":"NW1 6XE","country":"GB","defaultAddress":false}
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.defaultAddress", is(true)));

        // List addresses
        mockMvc.perform(bearer(get("/api/v1/users/me/addresses"), accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", is(1)))
                .andExpect(jsonPath("$.data[0].city", is("London")));

        // Refresh (rotation)
        MvcResult refreshResult =
                mockMvc.perform(
                                post("/api/v1/auth/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"refreshToken":"%s"}
                                                """
                                                        .formatted(refreshToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                        .andReturn();
        String rotatedRefresh =
                dataNode(refreshResult).get("refreshToken").asText();

        // Old refresh token is revoked after rotation
        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"refreshToken":"%s"}
                                        """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized());

        // Logout revokes the (rotated) refresh token
        mockMvc.perform(
                        bearer(post("/api/v1/auth/logout"), accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"refreshToken":"%s"}
                                        """.formatted(rotatedRefresh)))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"refreshToken":"%s"}
                                        """.formatted(rotatedRefresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsProfileAccessWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDuplicateRegistration() throws Exception {
        String email = "dup-" + System.nanoTime() + "@example.com";
        String body =
                """
                {"email":"%s","password":"%s","firstName":"Bob","lastName":"Brown"}
                """
                        .formatted(email, PASSWORD);

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsInvalidLogin() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"nobody@example.com","password":"Password123!"}
                                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidRegistrationPayload() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"not-an-email","password":"weak","firstName":"","lastName":""}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    private JsonNode login(String email) throws Exception {
        MvcResult result =
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
        return dataNode(result);
    }

    private JsonNode dataNode(MvcResult result) throws Exception {
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("data");
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder bearer(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
            String accessToken) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }
}