package com.company.ecommerce.notification.application;

import com.company.ecommerce.notification.domain.NotificationRecipient;
import com.company.ecommerce.notification.infrastructure.persistence.NotificationRecipientRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintains and resolves the notification module's local replica of recipient contact details. Keeps
 * the module free of any dependency on the {@code user} module: details are captured from
 * {@code UserRegisteredEvent} and looked up by the auth {@code userId} (== the {@code customerId} on
 * later business events).
 */
@Service
@RequiredArgsConstructor
public class RecipientDirectory {

    private final NotificationRecipientRepository recipientRepository;

    /** Inserts or updates the recipient replica for a user. */
    @Transactional
    public NotificationRecipient upsert(UUID userId, String email, String firstName) {
        return recipientRepository
                .findByUserId(userId)
                .map(
                        existing -> {
                            existing.update(email, firstName);
                            return existing;
                        })
                .orElseGet(
                        () ->
                                recipientRepository.save(
                                        NotificationRecipient.of(userId, email, firstName)));
    }

    /** Resolves a recipient by their user id, if known. */
    @Transactional(readOnly = true)
    public Optional<NotificationRecipient> findByUserId(UUID userId) {
        return recipientRepository.findByUserId(userId);
    }
}
