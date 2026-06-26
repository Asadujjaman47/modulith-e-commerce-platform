package com.company.ecommerce.notification.infrastructure.email;

/** Raised when an email cannot be handed off to the mail transport. */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
