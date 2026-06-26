package com.company.ecommerce.audit.application;

import com.company.ecommerce.audit.domain.AuditCategory;
import com.company.ecommerce.notification.domain.event.NotificationSentEvent;
import com.company.ecommerce.review.domain.event.ReviewCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Records audit entries for review and notification events. */
@Component
@RequiredArgsConstructor
public class EngagementAuditHandlers {

    private final AuditLogWriter audit;

    @ApplicationModuleListener
    public void on(ReviewCreatedEvent event) {
        audit.record(
                AuditCategory.REVIEW,
                "ReviewCreated",
                "CREATE",
                "Review",
                event.reviewId(),
                event.customerId(),
                "Review (%d star) created for product %s"
                        .formatted(event.rating(), event.productId()));
    }

    @ApplicationModuleListener
    public void on(NotificationSentEvent event) {
        // The notification type lives in notification's internal domain package (not its events named
        // interface), so we deliberately address the entry by recipient/reference only.
        audit.record(
                AuditCategory.NOTIFICATION,
                "NotificationSent",
                "SEND",
                "Notification",
                event.notificationId(),
                null,
                "Notification sent to %s (ref %s)"
                        .formatted(event.recipient(), event.referenceId()));
    }
}
