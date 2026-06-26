package com.company.ecommerce.notification.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.auth.domain.event.UserRegisteredEvent;
import com.company.ecommerce.notification.domain.NotificationRecipient;
import com.company.ecommerce.notification.domain.NotificationType;
import com.company.ecommerce.order.domain.event.OrderCreatedEvent;
import com.company.ecommerce.shipment.domain.event.ShipmentCreatedEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventHandlersTest {

    @Mock private RecipientDirectory recipientDirectory;
    @Mock private NotificationContentFactory contentFactory;
    @Mock private SendNotificationUseCase sendNotification;
    @InjectMocks private NotificationEventHandlers handlers;

    private final UUID userId = UUID.randomUUID();

    private NotificationRecipient recipient() {
        return NotificationRecipient.of(userId, "jane@example.com", "Jane");
    }

    @Test
    void welcomeUpsertsRecipientAndSends() {
        when(recipientDirectory.upsert(userId, "jane@example.com", "Jane")).thenReturn(recipient());
        when(contentFactory.welcome("Jane")).thenReturn(new NotificationContent("s", "b"));

        handlers.on(new UserRegisteredEvent(userId, "jane@example.com", "Jane", "Doe"));

        verify(sendNotification)
                .send(eq(NotificationType.WELCOME), eq("jane@example.com"), any(), eq(userId));
    }

    @Test
    void orderConfirmationSendsToResolvedRecipient() {
        UUID orderId = UUID.randomUUID();
        when(recipientDirectory.findByUserId(userId)).thenReturn(Optional.of(recipient()));
        when(contentFactory.orderConfirmation(any(), any(), any()))
                .thenReturn(new NotificationContent("s", "b"));

        handlers.on(
                new OrderCreatedEvent(
                        orderId, "ORD-1", userId, List.of(), null, BigDecimal.ZERO,
                        new BigDecimal("10.00")));

        verify(sendNotification)
                .send(
                        eq(NotificationType.ORDER_CONFIRMATION),
                        eq("jane@example.com"),
                        any(),
                        eq(orderId));
    }

    @Test
    void skipsWhenRecipientUnknown() {
        UUID shipmentId = UUID.randomUUID();
        when(recipientDirectory.findByUserId(userId)).thenReturn(Optional.empty());

        handlers.on(new ShipmentCreatedEvent(shipmentId, UUID.randomUUID(), userId, "TRK", "UPS"));

        verify(sendNotification, never()).send(any(), any(), any(), any());
    }
}
