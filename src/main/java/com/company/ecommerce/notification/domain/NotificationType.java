package com.company.ecommerce.notification.domain;

/**
 * The kind of business notification being sent, one per consumed event. Determines the subject/body
 * template used by the content factory.
 */
public enum NotificationType {
    WELCOME,
    ORDER_CONFIRMATION,
    PAYMENT_RECEIVED,
    SHIPMENT_CREATED,
    SHIPMENT_DELIVERED
}
