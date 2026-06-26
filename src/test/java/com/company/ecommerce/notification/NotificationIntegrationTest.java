package com.company.ecommerce.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.ecommerce.TestcontainersConfiguration;
import com.company.ecommerce.notification.domain.NotificationLog;
import com.company.ecommerce.notification.domain.NotificationStatus;
import com.company.ecommerce.notification.domain.NotificationType;
import com.company.ecommerce.notification.infrastructure.email.EmailSender;
import com.company.ecommerce.notification.infrastructure.persistence.NotificationLogRepository;
import com.company.ecommerce.notification.infrastructure.persistence.NotificationRecipientRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end coverage of the notification module against real PostgreSQL and Redis. Registering a
 * customer publishes {@code UserRegisteredEvent} through the real auth flow, which the notification
 * module consumes on a post-commit {@code @ApplicationModuleListener} to seed the recipient replica,
 * send a welcome email and append a {@code NotificationLog}. The {@link EmailSender} is replaced with a
 * capturing fake so message content can be asserted without a live SMTP server.
 *
 * <p>The per-event subject/body mapping for the order/payment/shipment notifications is covered by
 * {@code NotificationEventHandlersTest}; the welcome path here exercises the full event→email→log
 * wiring without publishing synthetic business events that other modules also consume.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, NotificationIntegrationTest.CapturingMailConfig.class})
class NotificationIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EmailSender emailSender;
    @Autowired private NotificationLogRepository notificationLogRepository;
    @Autowired private NotificationRecipientRepository recipientRepository;

    @Test
    void registrationTriggersWelcomeNotification() throws Exception {
        CapturingEmailSender sender = (CapturingEmailSender) emailSender;

        String email = "buyer-" + System.nanoTime() + "@example.com";
        UUID userId = register(email);

        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(
                        () -> {
                            // Recipient replica seeded from the event.
                            assertThat(recipientRepository.findByUserId(userId)).isPresent();
                            // Welcome email captured.
                            assertThat(sender.subjectsTo(email)).contains("Welcome to our store");
                            // Persisted as a SENT WELCOME notification log.
                            List<NotificationLog> logs =
                                    notificationLogRepository.findAll().stream()
                                            .filter(l -> email.equals(l.getRecipient()))
                                            .toList();
                            assertThat(logs).hasSize(1);
                            assertThat(logs.get(0).getType()).isEqualTo(NotificationType.WELCOME);
                            assertThat(logs.get(0).getStatus()).isEqualTo(NotificationStatus.SENT);
                        });
    }

    private UUID register(String email) throws Exception {
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
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        return UUID.fromString(data.get("userId").asText());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CapturingMailConfig {
        @Bean
        @Primary
        EmailSender capturingEmailSender() {
            return new CapturingEmailSender();
        }
    }

    /** Records sent messages in memory so assertions need no live SMTP server. */
    static class CapturingEmailSender implements EmailSender {
        private final List<Sent> sent = new CopyOnWriteArrayList<>();

        record Sent(String to, String subject, String body) {}

        @Override
        public void send(String to, String subject, String body) {
            sent.add(new Sent(to, subject, body));
        }

        List<String> subjectsTo(String to) {
            return sent.stream().filter(s -> s.to().equals(to)).map(Sent::subject).toList();
        }
    }
}
