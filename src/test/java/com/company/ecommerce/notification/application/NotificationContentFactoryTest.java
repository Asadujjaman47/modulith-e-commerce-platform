package com.company.ecommerce.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class NotificationContentFactoryTest {

    private final NotificationContentFactory factory = new NotificationContentFactory();

    @Test
    void welcomeGreetsByName() {
        NotificationContent content = factory.welcome("Jane");

        assertThat(content.subject()).isEqualTo("Welcome to our store");
        assertThat(content.body()).contains("Hi Jane");
    }

    @Test
    void welcomeFallsBackWhenNameMissing() {
        assertThat(factory.welcome(null).body()).contains("Hi there");
        assertThat(factory.welcome("  ").body()).contains("Hi there");
    }

    @Test
    void orderConfirmationIncludesNumberAndTotal() {
        NotificationContent content =
                factory.orderConfirmation("Jane", "ORD-123", new BigDecimal("49.90"));

        assertThat(content.subject()).isEqualTo("Order ORD-123 confirmed");
        assertThat(content.body()).contains("ORD-123").contains("49.90");
    }

    @Test
    void paymentReceivedIncludesAmountAndCurrency() {
        NotificationContent content =
                factory.paymentReceived("Jane", new BigDecimal("49.90"), "USD");

        assertThat(content.subject()).isEqualTo("Payment received");
        assertThat(content.body()).contains("49.90").contains("USD");
    }

    @Test
    void shipmentCreatedIncludesTrackingAndCarrier() {
        NotificationContent content = factory.shipmentCreated("Jane", "TRK-9", "UPS");

        assertThat(content.subject()).isEqualTo("Your order has shipped");
        assertThat(content.body()).contains("TRK-9").contains("UPS");
    }

    @Test
    void shipmentDeliveredAnnouncesDelivery() {
        NotificationContent content = factory.shipmentDelivered("Jane");

        assertThat(content.subject()).isEqualTo("Your order has been delivered");
        assertThat(content.body()).contains("delivered");
    }
}
