package com.company.ecommerce.notification.application;

import com.company.ecommerce.notification.domain.NotificationChannel;
import com.company.ecommerce.notification.domain.NotificationLog;
import com.company.ecommerce.notification.domain.NotificationType;
import com.company.ecommerce.notification.domain.event.NotificationSentEvent;
import com.company.ecommerce.notification.infrastructure.email.EmailDeliveryException;
import com.company.ecommerce.notification.infrastructure.email.EmailSender;
import com.company.ecommerce.notification.infrastructure.persistence.NotificationLogRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends a single email notification and records the outcome.
 *
 * <p>Delivery failures are caught and persisted as a {@code FAILED} {@link NotificationLog} rather than
 * propagated, so a mail outage can never roll back or block the business flow that triggered the
 * notification. A successful send publishes {@link NotificationSentEvent} for downstream consumers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendNotificationUseCase {

    private final EmailSender emailSender;
    private final NotificationLogRepository notificationLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void send(
            NotificationType type, String recipient, NotificationContent content, UUID referenceId) {
        try {
            emailSender.send(recipient, content.subject(), content.body());
            NotificationLog entry =
                    notificationLogRepository.save(
                            NotificationLog.sent(
                                    type,
                                    NotificationChannel.EMAIL,
                                    recipient,
                                    content.subject(),
                                    referenceId));
            eventPublisher.publishEvent(
                    new NotificationSentEvent(entry.getId(), type, recipient, referenceId));
            log.info(
                    "Notification sent. type={} recipient={} referenceId={}",
                    type,
                    recipient,
                    referenceId);
        } catch (EmailDeliveryException ex) {
            notificationLogRepository.save(
                    NotificationLog.failed(
                            type,
                            NotificationChannel.EMAIL,
                            recipient,
                            content.subject(),
                            referenceId,
                            ex.getMessage()));
            log.warn(
                    "Notification delivery failed. type={} recipient={} referenceId={} reason={}",
                    type,
                    recipient,
                    referenceId,
                    ex.getMessage());
        }
    }
}
