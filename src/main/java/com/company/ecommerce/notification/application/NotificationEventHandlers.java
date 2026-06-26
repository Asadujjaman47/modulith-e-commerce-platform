package com.company.ecommerce.notification.application;

import com.company.ecommerce.auth.domain.event.UserRegisteredEvent;
import com.company.ecommerce.notification.domain.NotificationRecipient;
import com.company.ecommerce.notification.domain.NotificationType;
import com.company.ecommerce.order.domain.event.OrderCreatedEvent;
import com.company.ecommerce.payment.domain.event.PaymentCompletedEvent;
import com.company.ecommerce.shipment.domain.event.ShipmentCreatedEvent;
import com.company.ecommerce.shipment.domain.event.ShipmentDeliveredEvent;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Translates business events from other modules into outbound notifications. Each listener runs after
 * the source transaction commits, in its own transaction ({@code @ApplicationModuleListener}).
 *
 * <p>The notification module depends on no other module: it consumes only the published {@code events}
 * named interfaces and resolves recipient addresses from its own {@link RecipientDirectory} replica
 * (seeded by {@link UserRegisteredEvent}). For order/payment/shipment events the {@code customerId}
 * equals the auth {@code userId} used as the replica key.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventHandlers {

    private final RecipientDirectory recipientDirectory;
    private final NotificationContentFactory contentFactory;
    private final SendNotificationUseCase sendNotification;

    @ApplicationModuleListener
    public void on(UserRegisteredEvent event) {
        NotificationRecipient recipient =
                recipientDirectory.upsert(event.userId(), event.email(), event.firstName());
        sendNotification.send(
                NotificationType.WELCOME,
                recipient.getEmail(),
                contentFactory.welcome(recipient.getFirstName()),
                event.userId());
    }

    @ApplicationModuleListener
    public void on(OrderCreatedEvent event) {
        recipient(event.customerId(), NotificationType.ORDER_CONFIRMATION, event.orderId())
                .ifPresent(
                        r ->
                                sendNotification.send(
                                        NotificationType.ORDER_CONFIRMATION,
                                        r.getEmail(),
                                        contentFactory.orderConfirmation(
                                                r.getFirstName(),
                                                event.orderNumber(),
                                                event.totalAmount()),
                                        event.orderId()));
    }

    @ApplicationModuleListener
    public void on(PaymentCompletedEvent event) {
        recipient(event.customerId(), NotificationType.PAYMENT_RECEIVED, event.paymentId())
                .ifPresent(
                        r ->
                                sendNotification.send(
                                        NotificationType.PAYMENT_RECEIVED,
                                        r.getEmail(),
                                        contentFactory.paymentReceived(
                                                r.getFirstName(), event.amount(), event.currency()),
                                        event.paymentId()));
    }

    @ApplicationModuleListener
    public void on(ShipmentCreatedEvent event) {
        recipient(event.customerId(), NotificationType.SHIPMENT_CREATED, event.shipmentId())
                .ifPresent(
                        r ->
                                sendNotification.send(
                                        NotificationType.SHIPMENT_CREATED,
                                        r.getEmail(),
                                        contentFactory.shipmentCreated(
                                                r.getFirstName(),
                                                event.trackingNumber(),
                                                event.carrier()),
                                        event.shipmentId()));
    }

    @ApplicationModuleListener
    public void on(ShipmentDeliveredEvent event) {
        recipient(event.customerId(), NotificationType.SHIPMENT_DELIVERED, event.shipmentId())
                .ifPresent(
                        r ->
                                sendNotification.send(
                                        NotificationType.SHIPMENT_DELIVERED,
                                        r.getEmail(),
                                        contentFactory.shipmentDelivered(r.getFirstName()),
                                        event.shipmentId()));
    }

    private Optional<NotificationRecipient> recipient(
            UUID customerId, NotificationType type, UUID referenceId) {
        Optional<NotificationRecipient> recipient = recipientDirectory.findByUserId(customerId);
        if (recipient.isEmpty()) {
            log.warn(
                    "No recipient on file for customer {}; skipping {} notification for {}.",
                    customerId,
                    type,
                    referenceId);
        }
        return recipient;
    }
}
