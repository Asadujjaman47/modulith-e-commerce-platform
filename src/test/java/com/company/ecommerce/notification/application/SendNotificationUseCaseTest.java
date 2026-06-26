package com.company.ecommerce.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.notification.domain.NotificationLog;
import com.company.ecommerce.notification.domain.NotificationStatus;
import com.company.ecommerce.notification.domain.NotificationType;
import com.company.ecommerce.notification.domain.event.NotificationSentEvent;
import com.company.ecommerce.notification.infrastructure.email.EmailDeliveryException;
import com.company.ecommerce.notification.infrastructure.email.EmailSender;
import com.company.ecommerce.notification.infrastructure.persistence.NotificationLogRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SendNotificationUseCaseTest {

    @Mock private EmailSender emailSender;
    @Mock private NotificationLogRepository notificationLogRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private SendNotificationUseCase useCase;

    private final UUID referenceId = UUID.randomUUID();
    private final NotificationContent content = new NotificationContent("Subject", "Body");

    @Test
    void recordsSentAndPublishesEventOnSuccess() {
        when(notificationLogRepository.save(any(NotificationLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        useCase.send(NotificationType.WELCOME, "jane@example.com", content, referenceId);

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(logCaptor.getValue().getRecipient()).isEqualTo("jane@example.com");

        ArgumentCaptor<NotificationSentEvent> eventCaptor =
                ArgumentCaptor.forClass(NotificationSentEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo(NotificationType.WELCOME);
        assertThat(eventCaptor.getValue().referenceId()).isEqualTo(referenceId);
    }

    @Test
    void recordsFailedAndPublishesNothingOnDeliveryFailure() {
        doThrow(new EmailDeliveryException("smtp down", new RuntimeException()))
                .when(emailSender)
                .send(any(), any(), any());

        useCase.send(NotificationType.WELCOME, "jane@example.com", content, referenceId);

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(logCaptor.getValue().getFailureReason()).contains("smtp down");
        verify(eventPublisher, never()).publishEvent(any());
    }
}
