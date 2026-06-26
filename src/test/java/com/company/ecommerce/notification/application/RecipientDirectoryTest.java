package com.company.ecommerce.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.notification.domain.NotificationRecipient;
import com.company.ecommerce.notification.infrastructure.persistence.NotificationRecipientRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecipientDirectoryTest {

    @Mock private NotificationRecipientRepository recipientRepository;
    @InjectMocks private RecipientDirectory recipientDirectory;

    private final UUID userId = UUID.randomUUID();

    @Test
    void insertsWhenRecipientUnknown() {
        when(recipientRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(recipientRepository.save(any(NotificationRecipient.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationRecipient result = recipientDirectory.upsert(userId, "jane@example.com", "Jane");

        assertThat(result.getEmail()).isEqualTo("jane@example.com");
        verify(recipientRepository).save(any(NotificationRecipient.class));
    }

    @Test
    void updatesExistingRecipientInPlace() {
        NotificationRecipient existing = NotificationRecipient.of(userId, "old@example.com", "Old");
        when(recipientRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        NotificationRecipient result = recipientDirectory.upsert(userId, "new@example.com", "New");

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getFirstName()).isEqualTo("New");
        verify(recipientRepository, never()).save(any());
    }
}
