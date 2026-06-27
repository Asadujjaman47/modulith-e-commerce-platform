package com.company.ecommerce.config.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.company.ecommerce.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Verifies the rate-limit filter against the real Redis container: once the strict per-IP auth limit
 * is spent, further login attempts get HTTP 429 with a {@code Retry-After} header, regardless of the
 * (failed) authentication outcome.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(
        properties = {
            "app.rate-limit.enabled=true",
            "app.rate-limit.auth.capacity=3",
            "app.rate-limit.auth.refill-period=PT1H"
        })
class RateLimitIntegrationTest {

    private static final String LOGIN_BODY =
            "{\"email\":\"ratelimit@example.com\",\"password\":\"WrongPassword1!\"}";

    @Autowired private MockMvc mockMvc;

    @Test
    void authEndpointReturns429AfterLimitExceeded() throws Exception {
        // The first 3 attempts are within the per-IP budget (they fail auth with 401, but are not
        // throttled).
        for (int i = 0; i < 3; i++) {
            int status = login().getResponse().getStatus();
            assertThat(status).isNotEqualTo(429);
        }

        // The 4th attempt exhausts the budget and is throttled.
        MvcResult throttled = login();
        assertThat(throttled.getResponse().getStatus()).isEqualTo(429);
        assertThat(throttled.getResponse().getHeader("Retry-After")).isNotNull();
        assertThat(throttled.getResponse().getContentAsString()).contains("Too many requests");
    }

    private MvcResult login() throws Exception {
        return mockMvc
                .perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(LOGIN_BODY))
                .andReturn();
    }
}
