package com.company.ecommerce.notification.application;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Builds the subject and plain-text body for each notification type from the triggering event's data.
 * Templating is kept deliberately simple (Java text blocks) to avoid an extra view-engine dependency.
 */
@Component
public class NotificationContentFactory {

    public NotificationContent welcome(String firstName) {
        return new NotificationContent(
                "Welcome to our store",
                """
                Hi %s,

                Thanks for creating an account with us. Happy shopping!
                """
                        .formatted(greetingName(firstName)));
    }

    public NotificationContent orderConfirmation(
            String firstName, String orderNumber, BigDecimal totalAmount) {
        return new NotificationContent(
                "Order %s confirmed".formatted(orderNumber),
                """
                Hi %s,

                We've received your order %s. Your order total is %s.
                We'll let you know once it ships.
                """
                        .formatted(greetingName(firstName), orderNumber, formatAmount(totalAmount)));
    }

    public NotificationContent paymentReceived(
            String firstName, BigDecimal amount, String currency) {
        return new NotificationContent(
                "Payment received",
                """
                Hi %s,

                We've received your payment of %s %s. Thank you!
                """
                        .formatted(greetingName(firstName), formatAmount(amount), currency));
    }

    public NotificationContent shipmentCreated(
            String firstName, String trackingNumber, String carrier) {
        return new NotificationContent(
                "Your order has shipped",
                """
                Hi %s,

                Good news — your order is on its way via %s.
                Tracking number: %s
                """
                        .formatted(greetingName(firstName), carrier, trackingNumber));
    }

    public NotificationContent shipmentDelivered(String firstName) {
        return new NotificationContent(
                "Your order has been delivered",
                """
                Hi %s,

                Your order has been delivered. We hope you enjoy it!
                """
                        .formatted(greetingName(firstName)));
    }

    private static String greetingName(String firstName) {
        return StringUtils.hasText(firstName) ? firstName : "there";
    }

    private static String formatAmount(BigDecimal amount) {
        return amount == null ? "0.00" : amount.toPlainString();
    }
}
