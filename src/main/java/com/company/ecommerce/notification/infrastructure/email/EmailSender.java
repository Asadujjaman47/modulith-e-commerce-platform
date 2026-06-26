package com.company.ecommerce.notification.infrastructure.email;

/**
 * Outbound email port. Implemented by {@link SmtpEmailSender}; abstracted so the notification use case
 * is independent of the transport and so tests can substitute a capturing fake.
 */
public interface EmailSender {

    /**
     * Sends a plain-text email.
     *
     * @throws EmailDeliveryException if the message could not be handed off to the mail transport
     */
    void send(String to, String subject, String body);
}
