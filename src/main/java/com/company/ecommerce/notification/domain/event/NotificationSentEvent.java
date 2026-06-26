package com.company.ecommerce.notification.domain.event;

import com.company.ecommerce.notification.domain.NotificationType;
import java.util.UUID;

/**
 * Published when a notification has been delivered successfully.
 *
 * <p>Consumed in later phases by {@code reporting}/{@code audit}. Carries the persisted log id, the
 * notification type, the recipient address and the id of the business entity that triggered it.
 */
public record NotificationSentEvent(
        UUID notificationId, NotificationType type, String recipient, UUID referenceId) {}
