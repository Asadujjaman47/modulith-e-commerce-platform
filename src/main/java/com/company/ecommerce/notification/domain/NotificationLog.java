package com.company.ecommerce.notification.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Append-only record of a single notification delivery attempt. Owned by the {@code notification}
 * module.
 *
 * <p>Captures what was sent, to whom, the outcome ({@code SENT}/{@code FAILED}) and a
 * {@code referenceId} linking back to the triggering business entity (order/payment/shipment) by id
 * value only — no cross-module FKs.
 */
@Entity
@Table(name = "notification_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationLog extends AuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private NotificationChannel channel;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "sent_at")
    private Instant sentAt;

    private NotificationLog(
            NotificationType type,
            NotificationChannel channel,
            String recipient,
            String subject,
            UUID referenceId) {
        this.type = type;
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.referenceId = referenceId;
    }

    /** Records a successfully delivered notification. */
    public static NotificationLog sent(
            NotificationType type,
            NotificationChannel channel,
            String recipient,
            String subject,
            UUID referenceId) {
        NotificationLog log = new NotificationLog(type, channel, recipient, subject, referenceId);
        log.status = NotificationStatus.SENT;
        log.sentAt = Instant.now();
        return log;
    }

    /** Records a notification that could not be delivered, with the failure reason. */
    public static NotificationLog failed(
            NotificationType type,
            NotificationChannel channel,
            String recipient,
            String subject,
            UUID referenceId,
            String failureReason) {
        NotificationLog log = new NotificationLog(type, channel, recipient, subject, referenceId);
        log.status = NotificationStatus.FAILED;
        log.failureReason = failureReason;
        return log;
    }
}
